package roiderUnion.nomads.old

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteFleetSpawner
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.FleetsHelper
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes
import roiderUnion.nomads.NomadsLevel

class GatheringFleetSpawner(private val nomadLevel: NomadsLevel) : RouteFleetSpawner {
    override fun spawnFleet(route: RouteData?): CampaignFleetAPI? {
        if (Helper.anyNull(route)) return null

        val primaryFaction = if (nomadLevel == NomadsLevel.UNION) RoiderFactions.ROIDER_UNION else Factions.INDEPENDENT
        val roiderEscort = nomadLevel.ordinal >= NomadsLevel.ALLIED.ordinal
        val factionParams = getFactionParams(primaryFaction, route!!.market?.factionId, roiderEscort)

        val fleet = FleetsHelper.createMultifactionFleet(
            createNomadParams(route),
            primaryFaction,
            *factionParams
        )

        Helper.addEntityToLocation(fleet, route.current?.containingLocationFrom, route.current?.from?.location)
        fleet.addScript(GatheringRouteFleetAssignmentAI(fleet, route))
//        fleet.addEventListener(this)
        Misc.makeLowRepImpact(fleet, RoiderFleetTypes.NOMAD_FLEET)
        return fleet
    }

    private fun getFactionParams(
        primaryFaction: String,
        marketFaction: String?,
        roiderEscort: Boolean
    ): Array<FleetsHelper.MultiFleetFactionParams> {
        val baseRoiderWeight = if (roiderEscort) 2f else 1f
        return when (marketFaction) {
            Factions.INDEPENDENT -> {
                arrayOf(
                    FleetsHelper.MultiFleetFactionParams(primaryFaction, 2f),
                    FleetsHelper.MultiFleetFactionParams(RoiderFactions.ROIDER_UNION, baseRoiderWeight)
                )

            }
            RoiderFactions.ROIDER_UNION -> {
                arrayOf(
                    FleetsHelper.MultiFleetFactionParams(primaryFaction, 1f),
                    FleetsHelper.MultiFleetFactionParams(RoiderFactions.ROIDER_UNION, baseRoiderWeight + 1f)
                )

            }
            null -> {
                arrayOf(
                    FleetsHelper.MultiFleetFactionParams(primaryFaction, 1f),
                    FleetsHelper.MultiFleetFactionParams(RoiderFactions.ROIDER_UNION, baseRoiderWeight)
                )
            }
            else -> {
                arrayOf(
                    FleetsHelper.MultiFleetFactionParams(primaryFaction, 1f),
                    FleetsHelper.MultiFleetFactionParams(RoiderFactions.ROIDER_UNION, baseRoiderWeight),
                    FleetsHelper.MultiFleetFactionParams(marketFaction, 1f)
                )

            }
        }
    }

    private fun createNomadParams(route: RouteData): FleetParamsV3 {
        val points = HashMap<FleetsHelper.Category, FleetsHelper.Weight>()
        points[FleetsHelper.Category.COMBAT] = FleetsHelper.Weight.EXTREME
        points[FleetsHelper.Category.FREIGHTER] = FleetsHelper.Weight.MID
        points[FleetsHelper.Category.TANKER] = FleetsHelper.Weight.MID
        points[FleetsHelper.Category.TRANSPORT] = FleetsHelper.Weight.LOW
        points[FleetsHelper.Category.LINER] = FleetsHelper.Weight.LOW
        points[FleetsHelper.Category.UTILITY] = FleetsHelper.Weight.LOW

        val fp = route.extra?.fp ?: 0f
//        model.totalFP += fp
//        model.currFP += fp

        val results = FleetsHelper.createFleetParams(
            route.random ?: Helper.random,
            points,
            fp,
            0f,
            route.qualityOverride,
            route.market,
            null,
            route.extra?.factionId ?: Factions.INDEPENDENT,
            route.extra?.fleetType ?: RoiderFleetTypes.NOMAD_FLEET,
        )

        results.timestamp = Global.getSector().clock.timestamp
        results.random = route.random ?: Helper.random
        if (route.market != null) results.modeOverride = Misc.getShipPickMode(route.market)

        return results
    }

    override fun shouldCancelRouteAfterDelayCheck(route: RouteData?): Boolean = false
    override fun shouldRepeat(route: RouteData?): Boolean = false

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteData?) {
//        if (Helper.anyNull(route)) return

        // When is this called? When it deflates the fleet?
    }

}