package roiderUnion.fleets.nomads

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager

class NomadExpeditionRouteManager : BaseRouteFleetManager(MIN_INTERVAL, MAX_INTERVAL) {
    companion object {
        const val ID = "roider_nomad_expeditions"
        const val MIN_INTERVAL = 0.01f
        const val MAX_INTERVAL = 0.01f
    }

    override fun addRouteFleetIfPossible() {
        // need source port
        // need destination port
        // need a target system somewhere in between
        // target system should be a minimum total distance from start and end systems
        // fleet must not travel through core worlds
        /*
            Nomad expedition fleet routing

            Split the sector into 9 sections.

            Use inhabited system positions to determine area of core world section

            nomads aren't allowed to travel to opposite sides of core worlds

            if start and end are on different sides, then target is always in a corner section, determined by which sides start and end are on


            Top-Left -> top-left corner
         */

        val startBase = pickStartingBase() ?: return
        val endBase = pickEndingBase(startBase) ?: return
        val targetSystem = pickTargetSystem(startBase, endBase) ?: return
    }

    private fun pickStartingBase(): SectorEntityToken? {
        return null
    }

    private fun pickEndingBase(startBase: SectorEntityToken): SectorEntityToken? {
        return startBase
    }

    private fun pickTargetSystem(startBase: SectorEntityToken, endBase: SectorEntityToken): StarSystemAPI? {
        return null
    }

    override fun spawnFleet(route: RouteManager.RouteData?): CampaignFleetAPI {
        TODO("Not yet implemented")
    }

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteManager.RouteData?) {}

    override fun getRouteSourceId(): String = ID

    override fun getMaxFleets(): Int {
        return 3
    }

    override fun shouldCancelRouteAfterDelayCheck(route: RouteManager.RouteData?): Boolean = false

    override fun shouldRepeat(route: RouteManager.RouteData?): Boolean = false
}