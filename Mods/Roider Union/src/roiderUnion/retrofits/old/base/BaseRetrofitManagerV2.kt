package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.HullModSpecAPI
import roiderUnion.ModPlugin
import roiderUnion.helpers.Helper
import starship_legends.RepRecord

open class BaseRetrofitManagerV2(val fitter: String, val entity: SectorEntityToken?) : RetrofitVerifier {
    companion object {
        const val HULL = "_HULL"
        const val BASE_CR = 0.7f
    }

    val retrofits: List<RetrofitData>
        get() = RetrofitsKeeper.getRetrofits(this, fitter)

    override fun verifyData(
        id: String?,
        fitter: String?,
        source: String?,
        target: String?,
        cost: Double,
        time: Double,
        rep: RepLevel?,
        commission: Boolean
    ): RetrofitData? {
        val c = if (entity?.market?.isPlanetConditionMarketOnly == false) {
            RetrofitsKeeper.calculateCost(source, target, entity.market)
        } else cost
        return RetrofitData(
            id!!, fitter!!, source!!, target!!, c,
            time, rep!!, commission
        )
    }

    fun deliverShip(source: FleetMemberAPI, target: String, toPlayer: Boolean) {
        val ship: FleetMemberAPI = Global.getFactory().createFleetMember(FleetMemberType.SHIP, target + HULL)
        ship.shipName = source.shipName
        transferSMods(source, ship)
        transferStarshipLegendsRep(source, ship)
        val storage: SubmarketAPI? = entity?.market?.getSubmarket(Submarkets.SUBMARKET_STORAGE)
        ship.repairTracker.cr = source.repairTracker?.cr ?: BASE_CR
        if (storage == null || toPlayer) {
            Helper.sector?.playerFleet?.fleetData?.addFleetMember(ship)
        } else {
            storage.cargo?.mothballedShips?.addFleetMember(ship)
        }
    }

    protected fun transferDMods(source: FleetMemberAPI, target: FleetMemberAPI) {
        val dMods = mutableListOf<HullModSpecAPI>()
        DModManager.getModsWithTags(Tags.HULLMOD_DMOD)
            .filter { source.variant?.hullMods?.contains(it.id) == true }
            .forEach { dMods += it }
        if (dMods.isEmpty()) return
        val targetVariant: ShipVariantAPI = target.variant.clone()
        targetVariant.originalVariant = null
        DModManager.setDHull(targetVariant)
        target.setVariant(targetVariant, false, true)
        val sourceDMods = dMods.size
        DModManager.removeUnsuitedMods(targetVariant, dMods)
        val addedDMods = dMods.size
        if (dMods.isNotEmpty()) {
            for (mod in dMods) {
                targetVariant.addPermaMod(mod.id)
            }

            if (sourceDMods > addedDMods) {
                DModManager.addDMods(target, false, sourceDMods - addedDMods, null)
            }
        } else if (sourceDMods > 0) {
            DModManager.addDMods(target, false, sourceDMods, null)
        }
    }

    protected fun transferSMods(source: FleetMemberAPI, target: FleetMemberAPI) {
        source.variant?.sMods?.forEach { target.variant?.addPermaMod(it, true) }
    }

    protected fun transferStarshipLegendsRep(source: FleetMemberAPI, target: FleetMemberAPI) {
        if (!ModPlugin.hasStarshipLegends) return
        if (!RepRecord.existsFor(source)) return
        RepRecord.transfer(source, target)
    }
}