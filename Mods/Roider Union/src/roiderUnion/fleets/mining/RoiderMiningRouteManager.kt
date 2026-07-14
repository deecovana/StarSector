package roiderUnion.fleets.mining

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.*
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.*
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes
import kotlin.math.min

class RoiderMiningRouteManager : BaseRouteFleetManager(MIN_INTERVAL, MAX_INTERVAL) {
    companion object {
        const val MIN_INTERVAL = 0.2f
        const val MAX_INTERVAL = 0.3f
        const val SOURCE_ID = "roider_miners"
        const val LIGHT_FP = 5f
        const val MEDIUM_FP = 30f
        const val HEAVY_FP = 60f

        const val HEAVY_LOAD_DAYS = 6f
        const val MEDIUM_LOAD_DAYS = 4f
        const val LIGHT_LOAD_DAYS = 2f

        const val HEAVY_STOPS = 3
        const val MEDIUM_STOPS = 2
        const val LIGHT_STOPS = 1

        const val MIN_GAP = 100f

        const val PREPARE = 0
        const val GO_TO1 = 1
        const val MINE1 = 2
        const val GO_TO2 = 3
        const val MINE2 = 4
        const val GO_TO3 = 5
        const val MINE3 = 6
        const val RETURN = 7
        const val UNLOAD = 8

        const val MAX_CARGO = 0.9f
        const val MACHINERY_CARGO = 0.1f
    }

    override fun getRouteSourceId(): String = SOURCE_ID

    override fun getMaxFleets(): Int {
        val numMarkets = Helper.sector?.economy?.marketsCopy?.count { Memory.isFlag(MemoryKeys.MINER_SOURCE, it) } ?: 0
        return min(numMarkets * 2, Settings.MAX_MINING_FLEETS)
    }

    override fun addRouteFleetIfPossible() {
        val source = getSourceMarket() ?: return
        val faction = pickFaction(source)
        val type = pickType()
        val dest = pickDest(source, type) ?: return
        val extra = OptionalFleetData(source, faction.id)
        extra.fleetType = type
        val route = getInstance().addRoute(
            routeSourceId,
            source,
            Helper.random.nextLong(),
            extra,
            this,
            dest
        ) ?: return
        addRouteSegments(route, source, dest)
        Memory.setFlag(MemoryKeys.RECENTLY_SENT_MINER, SOURCE_ID, source, Settings.TRADE_FLEET_INTERVAL)
    }

    private fun getSourceMarket(): MarketAPI? {
        val markets = Helper.sector?.economy?.marketsCopy
            ?.filter { Memory.isFlag(MemoryKeys.MINER_SOURCE, it) }
            ?.filterNot { Memory.isFlag(MemoryKeys.RECENTLY_SENT_MINER, it) } ?: return null
        if (markets.isEmpty()) return null
        return markets[Helper.random.nextInt(markets.size)]
    }

    private fun pickType(): String {
        val picker = WeightedRandomPicker<String>(Helper.random)
        picker.add(RoiderFleetTypes.MINER)
        picker.add(RoiderFleetTypes.MINING_FLEET)
        picker.add(RoiderFleetTypes.MINING_ARMADA)
        return picker.pick()
    }

    private fun pickFaction(source: MarketAPI): FactionAPI {
        val picker = WeightedRandomPicker<FactionAPI>(Helper.random)
        val indies = Helper.sector?.getFaction(Factions.INDEPENDENT)
        val pirates = Helper.sector?.getFaction(Factions.PIRATES)
        picker.add(source.faction)
        if (indies != null && !source.faction.isHostileTo(indies)) picker.add(indies)
        if (pirates != null && !source.faction.isHostileTo(pirates)) picker.add(pirates)
        return picker.pick()
    }

    private fun pickDest(source: MarketAPI, type: String): StarSystemAPI? {
        if (type == RoiderFleetTypes.MINER || type == RoiderFleetTypes.MINING_FLEET) return source.starSystem
        val systems = Helper.sector?.starSystems
            ?.filter { MiningHelper.inMiningRange(it, source.primaryEntity) }
            ?.filter { MiningHelper.canMine(it) }
            ?.filterNot { it === source.starSystem } ?: return null
        if (systems.isEmpty()) return null
        return systems[Helper.random.nextInt(systems.size)]
    }

    private fun addRouteSegments(route: RouteData, source: MarketAPI, dest: StarSystemAPI) {
        val daysToOrbit = when (route.extra.fleetType) {
            RoiderFleetTypes.MINER -> Helper.multHalfToFull(LIGHT_LOAD_DAYS, route.random)
            RoiderFleetTypes.MINING_FLEET -> Helper.multHalfToFull(MEDIUM_LOAD_DAYS, route.random)
            RoiderFleetTypes.MINING_ARMADA -> Helper.multHalfToFull(HEAVY_LOAD_DAYS, route.random)
            else -> 1f
        }
        route.addSegment(RouteSegment(PREPARE, daysToOrbit, source.primaryEntity))
        addMiningSegments(route, source, dest)
        route.addSegment(RouteSegment(UNLOAD, daysToOrbit * 2, source.primaryEntity))
    }

    private fun addMiningSegments(route: RouteData, source: MarketAPI, dest: StarSystemAPI) {
        val numStops = when (route.extra.fleetType) {
            RoiderFleetTypes.MINING_ARMADA -> HEAVY_STOPS
            RoiderFleetTypes.MINING_FLEET -> MEDIUM_STOPS
            else -> LIGHT_STOPS
        }
        var lastStop: SectorEntityToken? = null
        var index = 1
        while (index <= numStops) {
            val goToIndex = when (index) {
                3 -> GO_TO3
                2 -> GO_TO2
                else -> GO_TO1
            }
            lastStop = MiningHelper.addMiningStop(route, source, dest, goToIndex)
            index++
        }
        if (lastStop == null) return
        route.addSegment(RouteSegment(RETURN, lastStop, source.primaryEntity))
    }

    override fun spawnFleet(route: RouteData?): CampaignFleetAPI? {
        if (route == null) return null
        val faction = route.extra.factionId
        val type = route.extra.fleetType
        val fleet = createFleet(route, faction, type) ?: return null
        setCommander(fleet, type)
        setFlags(fleet, faction)
        fleet.addScript(RoiderMinerAssignmentAI(fleet, route))
        addCargo(route, fleet)
        return fleet
    }

    private fun createFleet(route: RouteData, faction: String, type: String): CampaignFleetAPI? {
        val source = route.market ?: return null
        val weightBonuses = MiningHelper.getMiningWeights(source.starSystem)
        val weights = getWeights(type)
        val fp = getFP(type, weightBonuses[faction] ?: 0f)
        val params = FleetsHelper.createFleetParams(
            Helper.random,
            weights,
            fp,
            0f,
            null,
            source,
            null,
            faction,
            type
        )
        val factionParams = FleetsHelper.getStandardFactionParams(faction)
        val result = FleetsHelper.createMultifactionFleet(
            params,
            faction,
            *factionParams
        )
        result.setFaction(faction, true)
        result.name = Helper.sector?.getFaction(faction)?.getFleetTypeName(type) ?: ExternalStrings.DEBUG_NULL
        return result
    }

    private fun getWeights(type: String): Map<FleetsHelper.Category, FleetsHelper.Weight> {
        return when (type) {
            RoiderFleetTypes.MINING_ARMADA -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.FREIGHTER, FleetsHelper.Weight.MID),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW)
                )
            }
            RoiderFleetTypes.MINING_FLEET -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.FREIGHTER, FleetsHelper.Weight.MID),
                    Pair(FleetsHelper.Category.TANKER, FleetsHelper.Weight.LOW)
                )
            }
            else -> {
                mapOf(
                    Pair(FleetsHelper.Category.COMBAT, FleetsHelper.Weight.EXTREME),
                    Pair(FleetsHelper.Category.FREIGHTER, FleetsHelper.Weight.MID)
                )
            }
        }
    }

    private fun getFP(type: String, weightBonus: Float): Float {
        return when (type) {
            RoiderFleetTypes.MINING_ARMADA -> {
                HEAVY_FP + Misc.random.nextFloat() * HEAVY_FP + weightBonus
            }
            RoiderFleetTypes.MINING_FLEET -> {
                MEDIUM_FP + Misc.random.nextFloat() * MEDIUM_FP + weightBonus
            }
            else -> {
                LIGHT_FP + Misc.random.nextFloat() * LIGHT_FP * 2f + weightBonus
            }
        }
    }

    private fun setCommander(fleet: CampaignFleetAPI, type: String) {
        val postId = Ranks.POST_FLEET_COMMANDER
        val rankId = when (type) {
            RoiderFleetTypes.MINER -> Ranks.CITIZEN
            RoiderFleetTypes.MINING_FLEET -> Ranks.SPACE_CAPTAIN
            RoiderFleetTypes.MINING_ARMADA -> Ranks.SPACE_ADMIRAL
            else -> Ranks.SPACE_COMMANDER
        }
        fleet.commander.postId = postId
        fleet.commander.rankId = rankId
    }

    private fun setFlags(fleet: CampaignFleetAPI, faction: String) {
        Memory.setFlag(MemFlags.MEMORY_KEY_TRADE_FLEET, fleet.id, fleet)
        if (Misc.isPirateFaction(fleet.faction)) {
            Memory.setFlag(MemFlags.MEMORY_KEY_PIRATE, fleet.id, fleet)
            Memory.setFlag(MemFlags.MEMORY_KEY_NO_REP_IMPACT, fleet.id, fleet)
        } else if (faction != RoiderFactions.ROIDER_UNION) {
            Memory.setFlag(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, fleet.id, fleet)
        }
    }

    private fun addCargo(route: RouteData, fleet: CampaignFleetAPI) {
        if (route.current.id > UNLOAD) return
        if (route.current.id >= GO_TO1) CargoHelper.addCommodity(Commodities.HEAVY_MACHINERY, fleet.cargo.maxCapacity * 0.1f, fleet.cargo)
        val seg = route.current.id
        val stops = when (route.extra.fleetType) {
            RoiderFleetTypes.MINING_ARMADA -> HEAVY_STOPS
            RoiderFleetTypes.MINING_FLEET -> MEDIUM_STOPS
            else -> LIGHT_STOPS
        }
        val mineDone = if (seg >= RETURN) {
            1f
        } else if (seg >= GO_TO3) {
            min(2f / stops, 1f)
        } else if (seg >= GO_TO2) {
            min(1f / stops, 1f)
        } else 0f
        MiningHelper.fillCargo(
            fleet,
            route.custom as StarSystemAPI,
            listOf(Commodities.ORE, Commodities.RARE_ORE, Commodities.ORGANICS, Commodities.VOLATILES),
            mineDone
        )
    }

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteData?) {}
    override fun shouldCancelRouteAfterDelayCheck(route: RouteData?): Boolean = false
    override fun shouldRepeat(route: RouteData?): Boolean = false
}