package roiderUnion.nomads.old

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.Helper

class GatheringRouteFleetAssignmentAI(fleet: CampaignFleetAPI, route: RouteData, delegate: FleetActionDelegate?) : RouteFleetAssignmentAI(fleet, route, delegate) {

    constructor(fleet: CampaignFleetAPI, route: RouteData) : this(fleet, route, null)

    override fun giveInitialAssignments() {
        val random = route.random ?: Helper.random
        route.current = getFirstSegment()
        route.current.elapsed = random.nextFloat() * route.current.daysMax
        super.giveInitialAssignments()
    }

    private fun getFirstSegment(): RouteSegment {
        val picker = WeightedRandomPicker<RouteSegment>(route.random ?: Helper.random)
        for (segment in route.segments) picker.add(segment, segment.daysMax)
        return picker.pick()
    }
}