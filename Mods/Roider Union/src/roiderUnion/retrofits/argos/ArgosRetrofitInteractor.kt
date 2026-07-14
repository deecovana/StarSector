package roiderUnion.retrofits.argos

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.CombatReadinessPlugin
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import retroLib.api.RetrofitDeliveryService
import retroLib.api.RetrofitManager
import retroLib.impl.BaseRetrofitPluginInteractor
import retroLib.impl.SourceTextData
import roiderUnion.helpers.Helper
import roiderUnion.ids.ShipsAndWings
import roiderUnion.ids.hullmods.RoiderHullmods

class ArgosRetrofitInteractor(
    private val aModel: ArgosRetrofitPluginModel,
    private val fleet: CampaignFleetAPI,
    manager: RetrofitManager,
    service: RetrofitDeliveryService,
    memoryMap: MutableMap<String, MemoryAPI>,
    originalPlugin: InteractionDialogPlugin?,
    showRetrofittingMessages: (Double, Boolean) -> Unit
) : BaseRetrofitPluginInteractor(aModel, fleet.faction, manager, service, memoryMap, originalPlugin, showRetrofittingMessages) {
    companion object {
        const val MACHINERY_FRACTION = 0.25f
        const val SUPPLIES_FRACTION = 0.5f
        const val METALS_FRACTION = 0.25f
        const val COM_CR = "cr_id"
        private val ARGO_SPEC: ShipHullSpecAPI? = Helper.settings?.getHullSpec(ShipsAndWings.ARGOS)
        val SUPPLIES_PER_CR = (ARGO_SPEC?.suppliesToRecover ?: 1f) / (ARGO_SPEC?.suppliesToRecover ?: 1f)
    }

    init {
        aModel.isPaidConversion = fleet != Helper.sector?.playerFleet
        aModel.factionId = faction.id
        aModel.factionDarkUIColor = faction.darkUIColor
        collectDocks()
        aModel.activeDockShips = aModel.availableDockShips
    }

    override fun update() {
        super.update()
        collectDocks()
    }

    private fun collectDocks() {
        val crPlugin = Helper.settings?.crPlugin
        val docks = ArrayList<FleetMemberAPI>()
        for (ship in fleet.fleetData?.membersListCopy ?: emptyList()) {
            if (ship.variant?.hasHullMod(RoiderHullmods.CONVERSION_DOCK) == true) {
                if (ship.isMothballed) continue
                val mal = crPlugin?.getMalfunctionThreshold(ship.stats) ?: 0f
                val cr = ship.repairTracker?.cr ?: continue
                if (cr <= mal) continue
                docks.add(ship)
            }
        }
        aModel.availableDockShips = docks
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (optionData == ArgosRetrofitView.OPTION_PICK_DOCKS) {
            model.optionId = optionData as String
            update()
            return
        }
        super.optionSelected(optionText, optionData)
    }

    override fun setConfirmationState() {
        super.setConfirmationState()
        aModel.needCR = !hasEnoughCR()
        if (!aModel.isPaidConversion) {
            aModel.rezCosts = getResourceCosts(model.currTarget!!.hullSpec!!.suppliesToRecover, model.confirmationCost.toDouble())
            aModel.totalRezCost = getTotalRezCost()
            aModel.needRez = !hasEnoughRez()
        }
    }

    private fun getTotalRezCost(): Int {
        return aModel.rezCosts.filter {
            it.key == Commodities.HEAVY_MACHINERY
                    || it.key == Commodities.SUPPLIES
                    || it.key == Commodities.METALS
        }.values.sum()
    }

    private fun hasEnoughCR(): Boolean {
        val crPlugin = Helper.settings?.crPlugin
        val crAvail = HashMap<FleetMemberAPI, Float>()
        for (dock in aModel.activeDockShips) {
            crAvail[dock] = getCRAvail(dock, crPlugin)
        }
        val crEach = model.currTarget?.hullSpec?.suppliesToRecover ?: return false
        var crTotal = crEach * model.selectedShips.size
        while (crTotal > 0) {
            crTotal -= crEach
            var afforded = false
            for (dock in crAvail.keys.toList()) {
                afforded = canDeductCR(crAvail[dock]!!, crEach)
                if (afforded) {
                    crAvail[dock] = crAvail[dock]!! - crEach
                }
            }
            if (!afforded) return false
        }
        return true
    }

    private fun hasEnoughRez(): Boolean {
        val cargo = Helper.sector?.playerFleet?.cargo ?: return false
        val tCost = model.confirmationCost.toDouble()
        if (tCost > 0) {
            val resources = getResourceCosts(model.currTarget!!.hullSpec!!.suppliesToRecover, tCost)
            if (!hasResource(Commodities.HEAVY_MACHINERY, cargo, resources)) return false
            if (!hasResource(Commodities.SUPPLIES, cargo, resources)) return false
            if (!hasResource(Commodities.METALS, cargo, resources)) return false
        }
        return true
    }

    private fun hasResource(id: String, cargo: CargoAPI, resources: Map<String, Int>): Boolean {
        return cargo.getCommodityQuantity(id) >= (resources[id] ?: 0)
    }

    override fun collectSourceCosts() {
        if (model.currTarget?.hullSpec == null) return
        model.factionName = faction.displayName
        val result = mutableListOf<ArgosSourceTextData>()
        val sources = manager.getSourcesData(model.currTarget)
        result += sources.filter { it.sourceSpec != null }.map {
            val data = SourceTextData(
                    it.sourceSpec!!.nameWithDesignationWithDashClass,
                    manager.isSourceAllowed(it),
                    manager.isSourceLegalRep(it),
                    manager.isSourceLegalCom(it),
                    it.reputation.displayName,
                    it.cost?.toInt() ?: 0,
                    it.time.toInt(),
                    getFrameReturnSize(it),
                    it.sourceSpec!!
                )
            val rezCosts = getResourceCosts(it.targetSpec.suppliesToRecover, it.cost ?: 0.0)
            ArgosSourceTextData(data, rezCosts)
        }
        result += sources.filter { it.sourceWingSpec != null }.map {
            val data = SourceTextData(
                    it.sourceWingSpec!!.wingName,
                    manager.isSourceAllowed(it),
                    manager.isSourceLegalRep(it),
                    manager.isSourceLegalCom(it),
                    it.reputation.displayName,
                    it.cost?.toInt() ?: 0,
                    it.time.toInt(),
                    getFrameReturnSize(it),
                    wingSpec = it.sourceWingSpec!!
                )
            val rezCosts = getResourceCosts(it.targetSpec.suppliesToRecover, it.cost!!)
            ArgosSourceTextData(data, rezCosts)
        }
        model.sourceTextData = result.map { it.data }
        aModel.argosSourceTextData = result
    }


    private fun getResourceCosts(crCost: Float, credits: Double): Map<String, Int> {
        val resources = mutableMapOf<String, Int>()
        resources[Commodities.CREDITS] = credits.toInt()
        resources[COM_CR] = crCost.toInt()
        if (aModel.isPaidConversion) return resources
        val crCredits = crCost * SUPPLIES_PER_CR * getCommodityCost(Commodities.SUPPLIES)
        val cost = credits - crCredits
        val machinery = cost / getCommodityCost(Commodities.HEAVY_MACHINERY) * MACHINERY_FRACTION
        resources[Commodities.HEAVY_MACHINERY] = machinery.toInt()
        val supplies = cost / getCommodityCost(Commodities.SUPPLIES) * SUPPLIES_FRACTION
        resources[Commodities.SUPPLIES] = supplies.toInt()
        val metals = cost / getCommodityCost(Commodities.METALS) * METALS_FRACTION
        resources[Commodities.METALS] = metals.toInt()
        return resources
    }

    private fun getCommodityCost(id: String): Float {
        val min = 1f
        return Helper.settings?.getCommoditySpec(id)?.basePrice?.coerceAtLeast(min) ?: min
    }

    override fun payRetrofitCost(cost: Double) {
        deductCR()
        if (aModel.isPaidConversion) {
            super.payRetrofitCost(cost)
        } else {
            payRezCosts(cost)
        }
    }

    private fun deductCR() {
        val crPlugin = Helper.settings?.crPlugin ?: return
        val crEach = model.currTarget?.hullSpec?.suppliesToRecover ?: return
        var crTotal = crEach * model.selectedShips.size
        for (dock in aModel.activeDockShips) {
            if (canDeductCR(getCRAvail(dock, crPlugin), crEach)) {
                val cr = (dock.repairTracker?.cr ?: 0f)
                dock.repairTracker?.cr = cr - (crEach / 100f)
                crTotal -= crEach
            }
            if (crTotal <= 0) return
        }
    }

    private fun getCRAvail(dock: FleetMemberAPI, crPlugin: CombatReadinessPlugin?): Float {
        val cr = dock.repairTracker?.cr ?: 0f
        val mal = crPlugin?.getMalfunctionThreshold(dock.stats) ?: 0f
        val result = if (aModel.isPaidConversion) cr - mal else cr
        return result * 100f
    }

    private fun canDeductCR(crAvail: Float, crEach: Float): Boolean {
        return crAvail >= crEach
    }

    private fun payRezCosts(cost: Double) {
        val resources = getResourceCosts(model.currTarget!!.hullSpec!!.suppliesToRecover, cost)
        val cargo = Helper.sector?.playerFleet?.cargo ?: return
        addRemoveCommodity(Commodities.HEAVY_MACHINERY, cargo, resources)
        addRemoveCommodity(Commodities.SUPPLIES, cargo, resources)
        addRemoveCommodity(Commodities.METALS, cargo, resources)
    }

    private fun addRemoveCommodity(id: String, cargo: CargoAPI, resources: Map<String, Int>) {
        val amount = resources[id]?.toFloat() ?: return
        if (amount >= 0) {
            cargo.removeCommodity(id, amount)
        } else {
            cargo.addCommodity(id, -amount)
        }

    }
}