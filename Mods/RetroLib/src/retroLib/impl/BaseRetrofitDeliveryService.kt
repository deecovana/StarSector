package retroLib.impl

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.util.Misc
import retroLib.*
import retroLib.api.RetrofitDeliveryService
import starship_legends.RepRecord

open class BaseRetrofitDeliveryService(protected val market: MarketAPI?) : RetrofitDeliveryService, EveryFrameScript {
    companion object {
        const val TOKEN_SHIP_NAME = "\$shipName"
        const val TOKEN_SHIP_CLASS = "\$shipClass"
    }

    override val isInstantOnly: Boolean = false
    override val queued = mutableListOf<RetrofitDelivery>()
    protected var done = false

    override fun queue(source: FleetMemberAPI?, data: RetrofitData?) {
        if (Helper.anyNull(source, data)) return
        val vData = RetrofitData(data!!)
        if (vData.time <= 0 || isInstantOnly) deliver(RetrofitDelivery(source!!, data))
        else queued += RetrofitDelivery(source!!, vData)
    }

    override fun prioritize(sourceId: String) {
        if (queued.isEmpty() || queued[0].source.id == sourceId) return
        val delivery = queued.firstOrNull { it.source.id == sourceId } ?: return
        queued.remove(delivery)
        queued.add(0, delivery)
    }

    override fun cancel(sourceId: String) {
        val delivery = queued.firstOrNull { it.source.id == sourceId } ?: return
        queued.remove(delivery)
    }
    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (queued.isEmpty()) return
        if (Helper.sector?.isPaused == true) return
        val days = Misc.getDays(amount)
        queued.forEach { it.advance(days) }
        val deliveries = queued.filter { it.ready }
        deliveries.forEach { deliver(it) }
        queued.removeAll(deliveries)
    }

    protected open fun deliver(delivery: RetrofitDelivery) {
        if (delivery.data.tags.contains(RetroLib_Tags.FIGHTER_WING)) {
            deliverFighterWing(delivery)
            return
        }
        val hullId = delivery.data.target + Helper.HULL_POSTFIX
        val ship = Helper.factory?.createFleetMember(FleetMemberType.SHIP, hullId) ?: return
        ship.shipName = delivery.source.shipName
//            transferDMods(delivery.source, ship);
        transferSMods(delivery.source, ship)
        transferStarshipLegendsRep(delivery.source, ship)
        ship.repairTracker.cr = delivery.source.repairTracker.cr
        val storage = market?.getSubmarket(Submarkets.SUBMARKET_STORAGE)
        if (storage == null || isInstantOnly) {
            Helper.sector?.playerFleet?.fleetData?.addFleetMember(ship)
        } else {
            storage.cargo?.mothballedShips?.addFleetMember(ship)
        }
        deliverFrameHull(delivery, ship.hullSpec.hullSize)
    }

    protected open fun deliverFighterWing(delivery: RetrofitDelivery) {
        val storage = market?.getSubmarket(Submarkets.SUBMARKET_STORAGE)
        if (storage == null || isInstantOnly) {
            Helper.sector?.playerFleet?.cargo?.addFighters(delivery.data.target, 1)
        } else {
            storage.cargo?.addFighters(delivery.data.target, 1)
        }
    }

    protected open fun transferSMods(source: FleetMemberAPI, target: FleetMemberAPI) {
        if (target.isFighterWing) return
        val sMods = source.variant?.sMods?.toList() ?: emptyList()
        Helper.sector?.playerStats?.setOnlyAddBonusXPDoNotSpendStoryPoints(true)
        val message = ExternalStrings.MESSAGE_BONUS_XP
            .replace(TOKEN_SHIP_NAME, source.shipName ?: ExternalStrings.DEBUG_NULL)
            .replace(TOKEN_SHIP_CLASS, source.hullSpec?.nameWithDesignationWithDashClass ?: ExternalStrings.DEBUG_NULL)
        Helper.sector?.playerStats?.setBonusXPGainReason(message)
        Helper.sector?.playerStats?.spendStoryPoints(
            sMods.size,
            true,
            null,
            true,
            Helper.getSModReturnBonusXP(source.hullSpec?.hullSize ?: HullSize.CAPITAL_SHIP, sMods.size),
            null
        )
        Helper.sector?.playerStats?.setOnlyAddBonusXPDoNotSpendStoryPoints(false)
        Helper.sector?.playerStats?.setBonusXPGainReason(null)
//        for (mod in sMods) {
//            target.variant.addPermaMod(mod, true)
//        }
    }

    protected fun transferStarshipLegendsRep(source: FleetMemberAPI, target: FleetMemberAPI) {
        if (target.isFighterWing) return
        if (!Settings.HAS_STARSHIP_LEGENDS) return
        if (!RepRecord.existsFor(source)) return
        RepRecord.transfer(source, target)
    }

    protected fun transferDMods(source: FleetMemberAPI, target: FleetMemberAPI) {
        if (target.isFighterWing) return
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

    protected open fun deliverFrameHull(delivery: RetrofitDelivery, targetsSize: HullSize) {
        val givenFrameHull = Helper.getGivenFrameHull(delivery.data.tags)
        val noFrame = !Helper.isFrameHull(delivery.source) && givenFrameHull == null
        if (noFrame) return
        val newSize = Helper.getFrameHullSize(delivery.source.hullSpec.hullSize, targetsSize)
        if (newSize == null && givenFrameHull == null) return
        val frameId = givenFrameHull
            ?: Helper.getFrameId(delivery.source.hullSpec.tags, newSize!!)
            ?: return
        val frame = Helper.factory?.createFleetMember(
            FleetMemberType.SHIP,
            frameId + Helper.HULL_POSTFIX
        ) ?: return
        val storage = market?.getSubmarket(Submarkets.SUBMARKET_STORAGE)
        if (storage == null || (delivery.data.time == 0.0 || isInstantOnly)) {
            Helper.sector?.playerFleet?.fleetData?.addFleetMember(frame)
        } else {
            storage.cargo?.mothballedShips?.addFleetMember(frame)
        }

    }
}