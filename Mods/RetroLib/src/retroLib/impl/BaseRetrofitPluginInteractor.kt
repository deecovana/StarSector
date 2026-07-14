package retroLib.impl

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import retroLib.*
import retroLib.api.RetrofitDeliveryService
import retroLib.api.RetrofitManager
import retroLib.api.RetrofitPluginInteractor
import retroLib.api.RetrofitPluginModel
import java.util.*

open class BaseRetrofitPluginInteractor(
    protected val model: RetrofitPluginModel,
    protected val faction: FactionAPI,
    protected val manager: RetrofitManager,
    protected val service: RetrofitDeliveryService,
    override val memoryMap: MutableMap<String, MemoryAPI>,
    protected val originalPlugin: InteractionDialogPlugin?,
    protected val showRetrofittingMessages: (Double, Boolean) -> Unit
): RetrofitPluginInteractor {
    override val context: Any?
        get() = null

    override fun init(dialog: InteractionDialogAPI) {
        model.dialog = dialog
        update()
    }

    open fun update() {
        handleQueuePriority()
        handleQueueCancel()
        updateText()
        updateOptions()
    }

    protected open fun handleQueuePriority() {
        if (model.optionId == OptionId.PRIORITIZE_QUEUED && model.selectedShips.isNotEmpty()) {
            model.selectedShips.forEach { service.prioritize(it.id) }
            model.selectedShips = listOf()
        }
    }

    protected open fun handleQueueCancel() {
        if (model.optionId == OptionId.CANCEL_QUEUED && model.selectedShips.isNotEmpty()) {
            model.selectedShips.forEach { service.cancel(it.id) }
            model.selectedShips.forEach { Helper.sector?.playerFleet?.fleetData?.addFleetMember(it) }
            model.selectedShips = listOf()
        }
    }

    protected open fun updateText() {
        model.factionColor = faction.baseUIColor
        if (service.queued.isNotEmpty()) {
            setFirstQueuedData()
            if (service.queued.size > 1) model.queuedDays = service.queued.sumOf { it.daysRemaining }
            setQueuedDisplay()
        }
        if (model.currTarget != null) {
            collectSourceCosts()
        } else if (service.queued.isEmpty()) {
            collectTargetSpecs()
        }
    }

    protected open fun updateOptions() {
        model.pickSourcesState = getPickSourcesState()
        setupIllegalRetrofitTooltip()
        setupNotAllowedRetrofitTooltip()
        model.targets = getRetrofitTargets()
        model.availableSources = manager.getAvailableSourceShips(model.currTarget)
        model.queueState = getQueueState()
        model.queued = service.queued.toList()
        if (model.selectedShips.isNotEmpty()) setConfirmationState()
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (optionData == null) {
            update()
            return
        }
        when (optionData) {
            OptionId.PICK_TARGET -> prepareRetrofitTargets()
            OptionId.PICK_SOURCES -> prepareRetrofitSources()
            OptionId.PRIORITIZE_QUEUED -> prioritizeQueued()
            OptionId.CANCEL_QUEUED -> cancelQueued()
            OptionId.CONFIRM_RETROFITS -> confirmRetrofits()
            OptionId.CANCEL_RETROFITS -> cancelRetrofits()
            OptionId.EXIT -> exit()
        }
    }

    protected fun getPickSourcesState(): PickSourcesOptionState {
        return if (manager.retrofits.isEmpty()) PickSourcesOptionState.NONE_AVAILABLE
        else if (model.currTarget == null) PickSourcesOptionState.NO_TARGET
        else if (!targetIsLegal) PickSourcesOptionState.ILLEGAL
        else if (!targetIsAllowed) PickSourcesOptionState.NOT_ALLOWED
        else PickSourcesOptionState.NORMAL
    }

    protected fun getQueueState(): QueueState {
        return if (service.queued.isEmpty()) QueueState.EMPTY
        else if (service.queued.size <= 1) QueueState.ONE
        else QueueState.MANY
    }

    protected open fun setConfirmationState() {
        var tCost = 0.0
        var refund = 0f
        model.selectedShips.forEach {
            refund += Helper.getSModReturnBonusXP (it.hullSpec?.hullSize ?: HullSize.CAPITAL_SHIP, it.variant?.sMods?.size ?: 0)
        }
        val queuedData = manager.getQueuedData(model.selectedShips, model.currTarget)
        queuedData.forEach { tCost += it.cost ?: 0.0 }
        model.confirmationCost = tCost.toFloat()
        model.sModRefundXP = refund
    }

    protected fun setFirstQueuedData() {
        val first = service.queued[0]
        model.firstSourceName = first.source.hullSpec?.nameWithDesignationWithDashClass ?: ExternalStrings.DEBUG_NULL
        model.firstTargetName = first.data.targetSpec.nameWithDesignationWithDashClass
        model.firstDaysRemaining = first.daysRemaining
    }

    protected fun setQueuedDisplay() {
        val members = mutableListOf<FleetMemberAPI>()
        service.queued.forEach {
            val m = Helper.factory?.createFleetMember(FleetMemberType.SHIP, it.data.target + Helper.HULL_POSTFIX)
            m?.shipName = it.source.shipName
            if (m != null) members += m
        }
        model.queuedMembers = members.toList()
    }

    protected open fun collectTargetSpecs() {
        model.targetsAvailable = manager.getAvailableTargets()
        model.targetsUnavailable = manager.getUnavailableTargets()
        model.targetsIllegal = manager.getIllegalTargets()
        model.targetsNotAllowed = manager.getNotAllowedTargets()
    }

    protected open fun collectSourceCosts() {
        if (model.currTarget?.hullSpec == null) return
        model.factionName = faction.displayName
        val result = mutableListOf<SourceTextData>()
        val sources = manager.getSourcesData(model.currTarget)
        result += sources.filter { it.sourceSpec != null }.map {
            SourceTextData(
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
        }
        result += sources.filter { it.sourceWingSpec != null }.map {
            SourceTextData(
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
        }
        model.sourceTextData = result
    }

    protected fun getFrameReturnSize(data: RetrofitData): HullSize? {
        if (Helper.isFrameHull(data.sourceSpec)) {
            return Helper.getFrameHullSize(data.sourceSpec!!.hullSize, data.targetSpec.hullSize)
        } else if (Helper.getGivenFrameHull(data.tags) != null) {
            data.tags.forEach {
                val size = when (it) {
                    RetroLib_Tags.GIVE_FRAME_CAPITAL -> HullSize.CAPITAL_SHIP
                    RetroLib_Tags.GIVE_FRAME_CRUISER -> HullSize.CRUISER
                    RetroLib_Tags.GIVE_FRAME_DESTROYER -> HullSize.DESTROYER
                    RetroLib_Tags.GIVE_FRAME_FRIGATE -> HullSize.FRIGATE
                    else -> null
                }
                if (size != null) return size
            }
        }
        return null
    }

    protected open fun prioritizeQueued() {
        model.optionId = OptionId.PRIORITIZE_QUEUED
        model.selectedShips = listOf()
    }

    protected open fun cancelQueued() {
        model.optionId = OptionId.CANCEL_QUEUED
        model.selectedShips = listOf()
    }

    protected open fun confirmRetrofits() {
        model.optionId = OptionId.CONFIRM_RETROFITS
        val dataToQueue = mutableMapOf<FleetMemberAPI, RetrofitData>()
        var cost = 0.0
        val queuedData = manager.getQueuedData(model.selectedShips, model.currTarget)
        model.selectedShips.forEach { m ->
            val data = queuedData.firstOrNull { it.source == m.hullId || it.source == m.hullSpec.baseHullId }
            if (data != null) {
                cost += data.cost ?: 0.0
                dataToQueue[m] = data
            }
        }

        payRetrofitCost(cost)

        val stripped = dataToQueue.keys.map { strip(it) }.any { it }
        dataToQueue.keys.filterNot { it.isFighterWing }.forEach { Helper.sector?.playerFleet?.fleetData?.removeFleetMember(it) }
        dataToQueue.keys.filter { it.isFighterWing }.forEach { Helper.sector?.playerFleet?.cargo?.removeFighters(it.specId, 1) }
        dataToQueue.keys.forEach { service.queue(it, dataToQueue[it]) }
        model.selectedShips = emptyList()
        showRetrofittingMessages(cost, stripped)
        update()
    }

    protected open fun payRetrofitCost(cost: Double) {
        if (cost > 0) {
            Helper.sector?.playerFleet?.cargo?.credits?.subtract(cost.toFloat())
        } else if (cost < 0) {
            Helper.sector?.playerFleet?.cargo?.credits?.add(-cost.toFloat())
        }
    }

    protected fun strip(ship: FleetMemberAPI): Boolean {
        val playerCargo: CargoAPI = Helper.sector?.playerFleet?.cargo ?: return false
        var result = false
        for (slot in ship.variant?.nonBuiltInWeaponSlots ?: listOf()) {
            if (ship.variant?.getWeaponId(slot).isNullOrEmpty()) continue
            playerCargo.addWeapons(ship.variant!!.getWeaponId(slot), 1)
            result = true
            ship.variant?.clearSlot(slot)
        }
        for (wing in ship.variant?.nonBuiltInWings ?: listOf()) {
            playerCargo.addFighters(wing, 1)
            result = true
        }
        ship.variant?.nonBuiltInWings?.clear()
        return result
    }

    protected open fun cancelRetrofits() {
        model.optionId = OptionId.CANCEL_RETROFITS
        model.selectedShips = listOf()
        update()
    }

    protected open fun prepareRetrofitTargets() {
        model.optionId = OptionId.PICK_TARGET
        model.targets = getRetrofitTargets()
    }

    protected open fun getRetrofitTargets(): List<FleetMemberAPI> {
        return manager.getAllTargets()
    }

    protected open fun prepareRetrofitSources() {
        model.optionId = OptionId.PICK_SOURCES
        model.availableSources = manager.getAvailableSourceShips(model.currTarget)
    }

    protected open fun exit() {
        model.optionId = OptionId.EXIT
        Timer().schedule(object : TimerTask() { override fun run() { finish() } }, 1)
    }

    protected open fun finish() {
        if (originalPlugin == null) {
            model.dialog.dismiss()
        } else {
            model.dialog.plugin = originalPlugin
            originalPlugin.optionSelected(null, Helper.BACK_TO_BAR)
        }
    }

    fun setupIllegalRetrofitTooltip() {
        val level = faction.getRelationshipLevel(Helper.sector?.playerFaction) ?: return
        model.isLowRep = !level.isAtWorst(targetRequiredLevel)
        model.needCommission = targetRequiresCommission && !Helper.hasCommission(faction.id)
    }

    open fun setupNotAllowedRetrofitTooltip() {}

    protected val targetIsLegal: Boolean
        get() = manager.isTargetLegal(model.currTarget?.hullId)

    protected val targetRequiredLevel: RepLevel
        get() = manager.getTargetRep(model.currTarget?.hullId)

    protected val targetRequiresCommission: Boolean
        get() = manager.isTargetCom(model.currTarget?.hullId)

    protected val targetIsAllowed: Boolean
        get() = manager.isTargetAllowed(model.currTarget?.hullId)

    override fun optionMousedOver(optionText: String?, optionData: Any?) {}
    override fun advance(amount: Float) {}
}