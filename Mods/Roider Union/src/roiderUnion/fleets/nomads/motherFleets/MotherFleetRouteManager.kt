package roiderUnion.fleets.nomads.motherFleets

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.OptionalFleetData
import roiderUnion.helpers.Helper
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.ids.*

class MotherFleetRouteManager : BaseRouteFleetManager(MIN_INTERVAL, MAX_INTERVAL) {
    companion object {
        const val MIN_INTERVAL = 0.1f
        const val MAX_INTERVAL = 1.0f
        const val ROUTE_ID = "roider_nomadMotherFleet"
        const val MAX_FLEETS = 3
        const val OFFERINGS_CHANCE = 0.8f

        const val UNION_SOURCE_WEIGHT = 1f
        const val NOMAD_BASE_SOURCE_WEIGHT = 2f

        @JvmField
        val FACTIONS = mutableSetOf(
            Factions.INDEPENDENT,
            Factions.PIRATES,
            RoiderFactions.ROIDER_UNION
        )
    }

    override fun getRouteSourceId(): String = ROUTE_ID

    override fun getMaxFleets(): Int = MAX_FLEETS

    override fun addRouteFleetIfPossible() {
        val random = Helper.random
        val extra = getOptionalFleetData()
        val faction = FACTIONS.toList()[random.nextInt(FACTIONS.size)]
        val offerings = getOfferings(faction)

        // Anyways, where exactly do mother fleets spawn?
        // - at markets with Union HQs
        // - at markets with Nomad Bazaari
        // What factions can they be
        // - indies, pirates, Roider Union
        // Where do they go
        // - systems where they can mine or salvage from planets
        // What do they do there
        // - hang out for ~4 months, "mining" or "salvaging"
        // Where do they end at
        // - a nomad base
        // - or a random roider market




        val route = RouteManager.getInstance().addRoute(
            routeSourceId,
            null,
            Misc.genRandomSeed(),
            extra,
            this,
            offerings)
    }

    private fun getSourceMarket(): MarketAPI? {
        val markets = Helper.sector?.economy?.marketsCopy ?: return null
        val picker = WeightedRandomPicker<MarketAPI>()
        for (market in markets) {
            val weight = if (market.hasFunctionalIndustry(RoiderIndustries.UNION_HQ)) UNION_SOURCE_WEIGHT
            else if (market.hasFunctionalIndustry(RoiderIndustries.NOMAD_BASE)) NOMAD_BASE_SOURCE_WEIGHT
            else continue
            picker.add(market, weight)
        }
        return null
    }

    private fun getOptionalFleetData(): OptionalFleetData {
        val result = OptionalFleetData()
        result.fleetType = RoiderFleetTypes.NOMAD_MOTHER_FLEET
        return result
    }

    private fun getOfferings(factionId: String): List<String> {
        val offerings = mutableListOf<String>()
        val random = Helper.random
        for (known in Helper.sector?.getFaction(factionId)?.knownShips ?: emptyList()) {
            val spec = Helper.settings?.getHullSpec(known) ?: continue
            if (!spec.hasTag(RoiderTags.RETROFIT)) continue
            if (random.nextFloat() > OFFERINGS_CHANCE) continue
            offerings.add(known)
        }
        for (known in Helper.roiders?.knownShips ?: emptyList()) {
            val spec = Helper.settings?.getHullSpec(known) ?: continue
            if (!spec.hasTag(RoiderTags.RETROFIT)) continue
            if (random.nextFloat() > OFFERINGS_CHANCE) continue
            offerings.add(known)
        }
        return offerings
    }

    override fun spawnFleet(route: RouteData?): CampaignFleetAPI? = MotherFleetSpawner.spawnFleet(route)

    override fun shouldCancelRouteAfterDelayCheck(route: RouteData?): Boolean = false
    override fun shouldRepeat(route: RouteData?): Boolean = false
    override fun reportAboutToBeDespawnedByRouteManager(route: RouteData?) {}
}