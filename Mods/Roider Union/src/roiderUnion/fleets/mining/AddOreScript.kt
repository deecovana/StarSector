package roiderUnion.fleets.mining

import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import roiderUnion.fleets.mining.RoiderMinerAssignmentAI.Companion.MAX_FILL
import roiderUnion.helpers.MiningHelper
import roiderUnion.ids.RoiderFleetTypes
import kotlin.reflect.KFunction1

class AddOreScript(
    private val fleet: CampaignFleetAPI,
    private val route: RouteData,
    private val current: RouteSegment,
    private val goNextScript: KFunction1<RouteSegment, Script>
) : Script {
    override fun run() {
        val locType = current.custom as? LocationType ?: LocationType.IN_ASTEROID_BELT
        val added = if (locType == LocationType.IN_SMALL_NEBULA) {
            listOf(Commodities.VOLATILES)
        } else {
            listOf(Commodities.ORE, Commodities.RARE_ORE, Commodities.ORGANICS)
        }
        val mult = when (route.extra.fleetType) {
            RoiderFleetTypes.MINING_ARMADA -> MAX_FILL / RoiderMiningRouteManager.HEAVY_STOPS
            RoiderFleetTypes.MINING_FLEET -> MAX_FILL / RoiderMiningRouteManager.MEDIUM_STOPS
            else -> MAX_FILL / RoiderMiningRouteManager.LIGHT_STOPS
        }
        MiningHelper.fillCargo(fleet, current.from.starSystem, added, mult)
        goNextScript(current).run()
    }
}