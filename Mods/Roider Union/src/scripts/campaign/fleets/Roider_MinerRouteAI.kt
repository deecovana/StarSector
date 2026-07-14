package scripts.campaign.fleets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import scripts.campaign.fleets.Roider_MinerRouteAI
import scripts.campaign.fleets.Roider_MinerRouteManager

/**
 * Author: SafariJohn
 */
class Roider_MinerRouteAI(
    fleet: CampaignFleetAPI?, route: RouteData?,
    private val supplyLevels: MutableMap<String, Int>?
) : RouteFleetAssignmentAI(fleet, route) {
    init {
        giveInitialAssignments()
    }

    override fun giveInitialAssignments() {
        if (supplyLevels == null) return
        if (supplyLevels.isEmpty()) supplyLevels[Commodities.ORE] = 1
        super.giveInitialAssignments()
    }

    override fun getStartingActionText(segment: RouteSegment): String {
        return "preparing for an expedition" // extern
    }

    override fun getInSystemActionText(segment: RouteSegment): String {
        if (segment.getId() == Roider_MinerRouteManager.PREPARE) {
            return "preparing for an expedition"
        }
        if (segment.getId() == Roider_MinerRouteManager.MINE) {
            return "mining " + cargoList
        }
        return if (segment.getId() == Roider_MinerRouteManager.UNLOAD) {
            "unloading " + cargoList
        } else "travelling"
    }

    override fun getTravelActionText(segment: RouteSegment): String {
        return if (segment.getId() == Roider_MinerRouteManager.RETURN) {
            "returning to " + route.market.name + " with " + cargoList
        } else {
            "travelling"
        }
    }

    override fun getEndingActionText(segment: RouteSegment): String {
        return if (segment.getId() == Roider_MinerRouteManager.UNLOAD) {
            "unloading " + cargoList
        } else super.getEndingActionText(segment)
        //To change body of generated methods, choose Tools | Templates.
    }

    override fun addEndingAssignment(current: RouteSegment, justSpawned: Boolean) {
        if (justSpawned) {
            val progress = current.progress
            RouteLocationCalculator.setLocation(
                fleet, progress,
                current.destination, current.destination
            )
        }
        var to = current.to
        if (to == null) to = current.from
        if (to == null) to = route.market.primaryEntity
        if (to == null || !to.isAlive) {
            val loc = Misc.getPointAtRadius(fleet.locationInHyperspace, 5000f)
            val token = Global.getSector().hyperspace.createToken(loc)
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, token, 1000f)
            return
        }
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION, to, 1000f,
            "returning to " + to.name
        )
        if (current.daysMax > current.elapsed) {
            fleet.addAssignment(
                FleetAssignment.ORBIT_PASSIVE, to,
                current.daysMax - current.elapsed, getEndingActionText(current)
            )
        }
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, to,
            1000f, super.getEndingActionText(current),
            goNextScript(current)
        )
    }

    // Should not happen
    private val cargoList: String
        private get() {
            val strings: MutableList<String> = ArrayList()
            for (cid in supplyLevels!!.keys) {
                if (supplyLevels[cid]!! > 0) strings.add(Global.getSettings().getCommoditySpec(cid).lowerCaseName)
            }
            if (strings.isEmpty()) strings.add(Global.getSettings().getCommoditySpec(Commodities.ORE).lowerCaseName)
            return if (strings.size > 1) Misc.getAndJoined(strings) else if (strings.size == 1) strings[0] else "" // Should not happen
        }

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_MinerRouteAI::class.java, "supplyLevels", "s")
        }
    }
}