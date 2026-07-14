package scripts.campaign.fleets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import org.lwjgl.util.vector.Vector2f
import roiderUnion.econ.DivesInteractor
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes
import scripts.Roider_Misc
import scripts.campaign.cleanup.Roider_MinerTokenCleaner
import java.util.*
import kotlin.math.roundToInt

/**
 * Author: SafariJohn
 */
class Roider_MinerRouteManager(market: MarketAPI, isUnionHQ: Boolean) : BaseRouteFleetManager(
    Global.getSettings().getFloat("averagePatrolSpawnInterval") * 0.7f,
    Global.getSettings().getFloat("averagePatrolSpawnInterval") * 1.3f
) {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_MinerRouteManager::class.java, "market", "m")
            x.aliasAttribute(Roider_MinerRouteManager::class.java, "random", "r")
            x.aliasAttribute(Roider_MinerRouteManager::class.java, "functional", "f")
            x.aliasAttribute(Roider_MinerRouteManager::class.java, "unionHQ", "u")
        }

        const val PREPARE = 0
        const val GO_TO = 1
        const val MINE = 2
        const val RETURN = 3
        const val UNLOAD = 4
        const val END = 5
        const val MINER_STARTING_FP = "\$roider_mining_starting_fp"
        private fun pickLocation(
            random: Random,
            system: StarSystemAPI?,
            gap: Float,
            exclude: Set<SectorEntityToken>,
            miningVolatiles: Boolean
        ): EntityLocation? {
            val weights: LinkedHashMap<LocationType, Float> = LinkedHashMap<LocationType, Float>()
            weights[LocationType.IN_ASTEROID_BELT] = 5f
            weights[LocationType.IN_ASTEROID_FIELD] = 5f
            weights[LocationType.IN_RING] = 5f
            weights[LocationType.PLANET_ORBIT] = 5f
            if (miningVolatiles) {
                weights[LocationType.IN_SMALL_NEBULA] = 5f
                weights[LocationType.GAS_GIANT_ORBIT] = 5f
            }
            val locs: WeightedRandomPicker<EntityLocation> =
                BaseThemeGenerator.getLocations(random, system, exclude, gap, weights)
            return if (locs.isEmpty) {
                null
            } else locs.pick()
        }
    }

    private val market: MarketAPI
    private val random: Random

    @Transient
    private var supplyLevels: MutableMap<String, Int>?
    private var functional: Boolean
    private var unionHQ: Boolean

    init {
        this.market = market
        random = Random()
        supplyLevels = HashMap()
        functional = false
        unionHQ = isUnionHQ
    }

    fun advance(
        amount: Float, supplyLevels: MutableMap<String, Int>?,
        functional: Boolean, unionHQ: Boolean
    ) {
        this.functional = functional
        this.supplyLevels = supplyLevels
        this.unionHQ = unionHQ
        super.advance(amount)
    }

    fun forceIntervalElapsed() {
        interval.forceIntervalElapsed()
    }

    override fun getRouteSourceId(): String {
        return "roider_miners_" + market.id
    }

    override fun getMaxFleets(): Int {
        if (true) return 0
        return if (unionHQ) 6 else 3
    }

    override fun addRouteFleetIfPossible() {
        if (!functional) return
        if (supplyLevels == null) supplyLevels = HashMap()
        val light = getCount(RoiderFleetTypes.MINER)
        val medium = getCount(RoiderFleetTypes.MINING_FLEET)
        val heavy = getCount(RoiderFleetTypes.MINING_ARMADA)
        var maxLight = 2
        var maxMedium = 1
        var maxHeavy = 0
        if (unionHQ) {
            maxLight++
            maxMedium++
            maxHeavy++
        }
        val picker = WeightedRandomPicker<String>()
        picker.add(RoiderFleetTypes.MINING_ARMADA, (maxHeavy - heavy).toFloat())
        picker.add(RoiderFleetTypes.MINING_FLEET, (maxMedium - medium).toFloat())
        picker.add(RoiderFleetTypes.MINER, (maxLight - light).toFloat())
        val type: String = picker.pick()
        val extra = RouteManager.OptionalFleetData(market)
        extra.fleetType = type
        if (picker.isEmpty) return
        var range: Float = DivesInteractor.DIVES_RANGE
        if (unionHQ) range = DivesInteractor.HQ_RANGE_BONUS

        // Create destination
        if (type == RoiderFleetTypes.MINER) range /= 2f // Halve range for small fleets
        var system: StarSystemAPI? = null
        var loc: EntityLocation? = null

        // Keep checking systems until we find a location
        // because there must be one
        val alreadyTried: MutableSet<StarSystemAPI?> = HashSet<StarSystemAPI?>()
        while (loc == null) {
            system = getHarvestableSystemInRange(alreadyTried, range)
            if (system == null) return
            alreadyTried.add(system)

            // Exclude any non-harvestable planets
//            val harvestMarkets: List<MarketAPI> = Roider_Dives.Companion.getHarvestTargetsInRange(market, range)
            val exclude: MutableSet<SectorEntityToken> = HashSet<SectorEntityToken>()
            for (planet in system.planets) {
                if (planet.isStar) continue
//                if (!harvestMarkets.contains(planet.market)) {
//                    exclude.add(planet)
//                }
            }
            loc = pickLocation(random, system, 200f, exclude, supplyLevels!!.containsKey(Commodities.VOLATILES))
        }
        assert(system != null)
        when (loc.type) {
            LocationType.GAS_GIANT_ORBIT, LocationType.PLANET_ORBIT -> {}
            LocationType.IN_SMALL_NEBULA -> {
                supplyLevels!!.remove(Commodities.ORE)
                supplyLevels!!.remove(Commodities.RARE_ORE)
                supplyLevels!!.remove(Commodities.ORGANICS)
            }

            LocationType.IN_ASTEROID_BELT, LocationType.IN_ASTEROID_FIELD, LocationType.IN_RING -> supplyLevels!!.remove(
                Commodities.VOLATILES
            )

            else -> supplyLevels!!.remove(Commodities.VOLATILES)
        }
        if (loc.orbit != null && (loc.type == LocationType.GAS_GIANT_ORBIT
                    || loc.type == LocationType.PLANET_ORBIT)
        ) {
            val planet = loc.orbit.focus
            var hasOre = false
            var hasRareOre = false
            var hasOrganics = false
            var hasVolatiles = false
            for (c in planet.market.conditions) {
                when (c.id) {
                    Conditions.ORE_ULTRARICH, Conditions.ORE_RICH, Conditions.ORE_ABUNDANT, Conditions.ORE_MODERATE -> hasOre =
                        true

                    Conditions.ORE_SPARSE -> {}
                    Conditions.ORGANICS_PLENTIFUL, Conditions.ORGANICS_ABUNDANT, Conditions.ORGANICS_COMMON -> hasOrganics =
                        true

                    Conditions.ORGANICS_TRACE -> {}
                    Conditions.RARE_ORE_ULTRARICH, Conditions.RARE_ORE_RICH -> hasRareOre = true
                    Conditions.RARE_ORE_ABUNDANT -> {}
                    Conditions.RARE_ORE_MODERATE -> {}
                    Conditions.RARE_ORE_SPARSE -> {}
                    Conditions.VOLATILES_PLENTIFUL -> hasVolatiles = true
                    Conditions.VOLATILES_ABUNDANT -> {}
                    Conditions.VOLATILES_DIFFUSE -> {}
                    Conditions.VOLATILES_TRACE -> {}
                }
            }
            if (!hasOre) supplyLevels!!.remove(Commodities.ORE)
            if (!hasRareOre) supplyLevels!!.remove(Commodities.RARE_ORE)
            if (!hasOrganics) supplyLevels!!.remove(Commodities.ORGANICS)
            if (!hasVolatiles) supplyLevels!!.remove(Commodities.VOLATILES)
        }
        val route: RouteManager.RouteData = RouteManager.getInstance().addRoute(
            routeSourceId, market, random.nextLong(),
            extra, this, supplyLevels
        )
        val dest: SectorEntityToken = system!!.createToken(Vector2f())
        system.addEntity(dest)
        BaseThemeGenerator.setEntityLocation(dest, loc, null)

        // Script will clean up token when route is no longer active
        Global.getSector().addScript(Roider_MinerTokenCleaner(route, dest))
        var daysToOrbit = getDaysToOrbit(type) * 0.25f
        if (daysToOrbit < 0.2f) {
            daysToOrbit = 0.2f
        }
        var dist: Float = Misc.getDistanceLY(market.locationInHyperspace, dest.locationInHyperspace)
        if (dist == 0f) dist = Misc.getDistance(route.market.primaryEntity, dest)
        val travelDays = dist * 1.5f
        val daysToMine = getDaysToMine(type)
        route.addSegment(RouteManager.RouteSegment(PREPARE, daysToOrbit, market.primaryEntity))
        route.addSegment(RouteManager.RouteSegment(GO_TO, travelDays, market.primaryEntity, dest))
        route.addSegment(RouteManager.RouteSegment(MINE, daysToMine, dest))
        route.addSegment(RouteManager.RouteSegment(RETURN, travelDays, dest, market.primaryEntity))
        route.addSegment(RouteManager.RouteSegment(UNLOAD, daysToOrbit * 2, market.primaryEntity))
        //		route.addSegment(new RouteSegment(END, Short.MAX_VALUE, market.getPrimaryEntity()));
    }

    private fun getCount(vararg types: String): Int {
        var count = 0
        for (route in RouteManager.getInstance().getRoutesForSource(routeSourceId)) {
            val cType: String = route.extra.fleetType
            //            String cType = route.getActiveFleet().getMemoryWithoutUpdate().getString(MemFlags.MEMORY_KEY_FLEET_TYPE);
            for (type in types) {
                if (type == cType) {
                    count++
                    break
                }
            }
        }
        return count
    }

    private fun getDaysToOrbit(fleetType: String): Float {
        var daysToOrbit = 0f
        when (fleetType) {
            RoiderFleetTypes.MINER -> daysToOrbit += 2f
            RoiderFleetTypes.MINING_FLEET -> daysToOrbit += 4f
            RoiderFleetTypes.MINING_ARMADA -> daysToOrbit += 6f
        }
        daysToOrbit *= (0.5f + Math.random().toFloat() * 0.5f)
        return daysToOrbit
    }

    private fun getDaysToMine(fleetType: String): Float {
        var daysToMine = 10f
        when (fleetType) {
            RoiderFleetTypes.MINER -> daysToMine += 5f
            RoiderFleetTypes.MINING_FLEET -> daysToMine += 10f
            RoiderFleetTypes.MINING_ARMADA -> daysToMine += 15f
        }
        daysToMine *= (0.5f + Math.random().toFloat() * 0.5f)
        return daysToMine
    }

    override fun spawnFleet(route: RouteManager.RouteData): CampaignFleetAPI? {
        var combat = 0f
        var tanker = 0f
        var freighter = 0f
        when (route.extra.fleetType) {
            RoiderFleetTypes.MINER -> {
                combat = (1f + random.nextFloat() * 2f).roundToInt() * 5f
                freighter = random.nextFloat().roundToInt() * 5f
            }

            RoiderFleetTypes.MINING_FLEET -> {
                combat = (3f + random.nextFloat() * 3f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 5f
                freighter = random.nextFloat().roundToInt() * 15f
            }

            RoiderFleetTypes.MINING_ARMADA -> {
                combat = (5f + random.nextFloat() * 5f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 10f
                freighter = random.nextFloat().roundToInt() * 25f
            }
        }
        val fleet: CampaignFleetAPI?
        if (route.factionId != RoiderFactions.ROIDER_UNION) {
            // Get half the ships from Roider Union's choices
            val params = FleetParamsV3(
                market,
                null,  // loc in hyper; don't need if have market
                RoiderFactions.ROIDER_UNION,
                null,  // quality override
                route.extra.fleetType,
                combat / 2,  // combatPts
                freighter / 2,  // freighterPts
                tanker / 2,  // tankerPts
                0f,  // transportPts
                0f,  // linerPts
                0f,  // utilityPts
                0f // qualityMod
            )
            params.timestamp = Global.getSector().clock.timestamp
            params.random = random
            //		params.modeOverride = Misc.getShipPickMode(market);
            params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL
            fleet = FleetFactoryV3.createFleet(params)
            if (fleet == null || fleet.isEmpty) return null


            // Get the other half from the market's faction
            val params2 = FleetParamsV3(
                market,
                null,  // loc in hyper; don't need if have market
                null,
                null,  // quality override
                route.extra.fleetType,
                combat / 2,  // combatPts
                freighter / 2,  // freighterPts
                tanker / 2,  // tankerPts
                0f,  // transportPts
                0f,  // linerPts
                0f,  // utilityPts
                0f // qualityMod
            )
            params2.timestamp = Global.getSector().clock.timestamp
            params2.random = random
            //		params.modeOverride = Misc.getShipPickMode(market);
            params2.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL
            val fleet2: CampaignFleetAPI? = FleetFactoryV3.createFleet(params2)
            if (fleet2 != null && !fleet2.isEmpty) {
                for (member in fleet2.membersWithFightersCopy) {
                    if (member.isFighterWing) continue
                    fleet.fleetData.addFleetMember(member)
                }
            }


            //        fleet.getMemoryWithoutUpdate().set(MINER_STARTING_FP, fleet.getFleetPoints());
            FleetFactoryV3.addCommanderAndOfficers(fleet, params, random)
            Roider_Misc.sortFleetByShipSize(fleet)
        } else {
            val params = FleetParamsV3(
                market,
                null,  // loc in hyper; don't need if have market
                RoiderFactions.ROIDER_UNION,
                null,  // quality override
                route.extra.fleetType,
                combat,  // combatPts
                freighter,  // freighterPts
                tanker,  // tankerPts
                0f,  // transportPts
                0f,  // linerPts
                0f,  // utilityPts
                0f // qualityMod
            )
            params.timestamp = Global.getSector().clock.timestamp
            params.random = random
            //		params.modeOverride = Misc.getShipPickMode(market);
            params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL
            fleet = FleetFactoryV3.createFleet(params)
            if (fleet == null || fleet.isEmpty) return null
        }
        fleet.setFaction(market.factionId, true)
        fleet.name = market.faction.getFleetTypeName(route.extra.fleetType)
        //		fleet.setNoFactionInName(true);
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_TRADE_FLEET, true)
        val postId = Ranks.POST_FLEET_COMMANDER
        var rankId = Ranks.SPACE_COMMANDER
        when (route.extra.fleetType) {
            RoiderFleetTypes.MINER -> rankId = Ranks.CITIZEN
            RoiderFleetTypes.MINING_FLEET -> rankId = Ranks.SPACE_CAPTAIN
            RoiderFleetTypes.MINING_ARMADA -> rankId = Ranks.SPACE_ADMIRAL
        }
        fleet.commander.postId = postId
        fleet.commander.rankId = rankId

//		market.getContainingLocation().addEntity(fleet);
//		fleet.setFacing((float) Math.random() * 360f);
//		fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);
        if (Misc.isPirateFaction(fleet.faction)) {
            fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_PIRATE, true)
        }

//        fleet.addScript(new Roider_MinerAssignmentAI(fleet, market, dest, supplyLevels));
        fleet.addScript(Roider_MinerRouteAI(fleet, route, route.custom as MutableMap<String, Int>))
        return fleet
    }

    override fun shouldCancelRouteAfterDelayCheck(route: RouteManager.RouteData): Boolean {
        return false
    }

    override fun shouldRepeat(route: RouteManager.RouteData): Boolean {
        return false
    }

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteManager.RouteData) {}
    private fun getHarvestableSystemInRange(alreadyTried: Set<StarSystemAPI?>, ly: Float): StarSystemAPI? {
        val loc: Vector2f = market.locationInHyperspace

        // What systems are in range?
        val targets: WeightedRandomPicker<StarSystemAPI> = WeightedRandomPicker<StarSystemAPI>()
        for (s in Global.getSector().starSystems) {
            if (Helper.isSpecialSystem(s)) continue
            if (alreadyTried.contains(s)) continue
            if (s.hasTag(Tags.THEME_UNSAFE)) continue
            val a: Float = loc.getX() - s.hyperspaceAnchor.locationInHyperspace.getX()
            val b: Float = loc.getY() - s.hyperspaceAnchor.locationInHyperspace.getY()
            val c = a * a + b * b
            if (c == 0f) {
                targets.add(s)
                continue
            }
            val lyDist = Global.getSettings().unitsPerLightYear
            val inRange = c <= lyDist * lyDist * ly * ly
            if (inRange) targets.add(s)
        }
        return if (targets.isEmpty) null else targets.pick()
    }
}