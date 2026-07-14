package scripts.campaign.fleets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderFleetTypes
import scripts.Roider_Misc
import scripts.campaign.ai.Roider_MinerAssignmentAI
import java.util.*
import kotlin.math.roundToInt

/**
 * Author: SafariJohn
 */
class Roider_MinerManager(market: MarketAPI?) : FleetEventListener {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_MinerManager::class.java, "tracker", "t")
            x.aliasAttribute(Roider_MinerManager::class.java, "market", "m")
            x.aliasAttribute(Roider_MinerManager::class.java, "miners", "f")
            x.aliasAttribute(Roider_MinerManager::class.java, "random", "rand")
            x.aliasAttribute(Roider_MinerManager::class.java, "returningMinerValue", "r")
            x.aliasAttribute(Roider_MinerManager::class.java, "functional", "n")
        }

        const val MINER_STARTING_FP = "\$roider_mining_starting_fp"
        private fun pickLocation(
            random: Random,
            system: StarSystemAPI,
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

    //    public static final int ROIDER_PREPARE = 1;
    //    public static final int ROIDER_TRAVEL_TO = 2;
    //    public static final int ROIDER_MINE_COMMON = 3;
    //    public static final int ROIDER_MINE_RARE = 4;
    //    public static final int ROIDER_RETURN = 5;
    //    public static final int ROIDER_UNLOAD = 6;
    private val tracker: IntervalUtil = IntervalUtil(
        Global.getSettings().getFloat("averagePatrolSpawnInterval") * 0.7f,
        Global.getSettings().getFloat("averagePatrolSpawnInterval") * 1.3f
    )
    private val market: MarketAPI?
    private val miners: MutableList<CampaignFleetAPI?>
    private val random: Random
    private var returningMinerValue: Float
    private var functional: Boolean

    init {
        this.market = market
        miners = ArrayList<CampaignFleetAPI?>()
        random = Random()
        returningMinerValue = 0f
        functional = true
    }

    fun getTracker(): IntervalUtil {
        return tracker
    }

    fun advance(
        unionHQ: Boolean, amount: Float, functional: Boolean,
        range: Float, supplyLevels: MutableMap<String, Int>
    ) {
        var range = range
        this.functional = functional
        if (!functional) return
        if (market?.isInEconomy == false) return
        var spawnRate = 1f
        val rateMult: Float
        rateMult = if (market != null) market.stats.dynamic.getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT)
            .modifiedValue else 1f
        spawnRate *= rateMult
        val days = Global.getSector().clock.convertToDays(amount)
        var extraTime = 0f
        if (returningMinerValue > 0) {
            // apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
            val interval: Float = tracker.intervalDuration
            extraTime = interval * days
            returningMinerValue -= days
            if (returningMinerValue < 0) returningMinerValue = 0f
        }
        tracker.advance(days * spawnRate + extraTime)

        //tracker.advance(days * spawnRate * 100f);
        if (tracker.intervalElapsed()) {
            // Clean up orphaned fleets
//            List<CampaignFleetAPI> remove = new ArrayList<>();
//            for (CampaignFleetAPI fleet : miners) {
//                if (fleet.getContainingLocation() == null ||
//                    !fleet.getContainingLocation().getFleets().contains(fleet)) {
//                    remove.add(fleet);
//                }
//            }
//            miners.removeAll(remove);
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
            if (picker.isEmpty) return
            val fleet: CampaignFleetAPI = spawnFleet(type) ?: return
            miners.add(fleet)

            // Create destination
            if (type == RoiderFleetTypes.MINER) range /= 2f // Halve range for small fleets
            var system: StarSystemAPI? = null
            var loc: EntityLocation? = null

            // Keep checking systems until we find a location
            // because there must be one
            val alreadyTried: MutableSet<StarSystemAPI> = HashSet()
            while (loc == null) {
                system = getHarvestableSystemInRange(alreadyTried, range)
                if (system == null) return
                alreadyTried.add(system)

                // Exclude any non-harvestable planets
//                val harvestMarkets: List<MarketAPI> = Roider_Dives.getHarvestTargetsInRange(market, range)
                val exclude: MutableSet<SectorEntityToken> = HashSet<SectorEntityToken>()
                for (planet in system.planets) {
//                    if (!harvestMarkets.contains(planet.market)) {
//                        exclude.add(planet)
//                    }
                }
                loc = pickLocation(random, system, 200f, exclude, supplyLevels.containsKey(Commodities.VOLATILES))
            }
            assert(system != null)
            when (loc.type) {
                LocationType.GAS_GIANT_ORBIT, LocationType.PLANET_ORBIT -> {}
                LocationType.IN_SMALL_NEBULA -> {
                    supplyLevels.remove(Commodities.ORE)
                    supplyLevels.remove(Commodities.RARE_ORE)
                    supplyLevels.remove(Commodities.ORGANICS)
                }

                LocationType.IN_ASTEROID_BELT, LocationType.IN_ASTEROID_FIELD, LocationType.IN_RING -> supplyLevels.remove(
                    Commodities.VOLATILES
                )

                else -> supplyLevels.remove(Commodities.VOLATILES)
            }
            if (loc.type == LocationType.GAS_GIANT_ORBIT
                || loc.type == LocationType.PLANET_ORBIT
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
                if (!hasOre) supplyLevels.remove(Commodities.ORE)
                if (!hasRareOre) supplyLevels.remove(Commodities.RARE_ORE)
                if (!hasOrganics) supplyLevels.remove(Commodities.ORGANICS)
                if (!hasVolatiles) supplyLevels.remove(Commodities.VOLATILES)
            }
            val dest: SectorEntityToken = system!!.createToken(Vector2f())
            system.addEntity(dest)
            BaseThemeGenerator.setEntityLocation(dest, loc, null)
            if (Misc.isPirateFaction(fleet.faction)) {
                fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_PIRATE, true)
            }
            fleet.addScript(Roider_MinerAssignmentAI(fleet, market!!, dest, supplyLevels))


//            route.addSegment(new RouteSegment(ROIDER_PREPARE, orbitDays, market.getPrimaryEntity()));
//            route.addSegment(new RouteSegment(ROIDER_TRAVEL_TO, market.getPrimaryEntity(), dest));
//            route.addSegment(new RouteSegment(ROIDER_MINE_COMMON, orbitDays * 5f, dest));
//            if (rareCargo) route.addSegment(new RouteSegment(ROIDER_MINE_RARE, orbitDays * 5f, dest));
//            route.addSegment(new RouteSegment(ROIDER_RETURN, dest, market.getPrimaryEntity()));
//            route.addSegment(new RouteSegment(ROIDER_UNLOAD, orbitDays, market.getPrimaryEntity()));
        }
    }

    private fun getCount(vararg types: String): Int {
        var count = 0
        for (fleet in miners) {
            val cType: String = fleet!!.memoryWithoutUpdate.getString(MemFlags.MEMORY_KEY_FLEET_TYPE)
            for (type in types) {
                if (type == cType) {
                    count++
                    break
                }
            }
        }
        return count
    }

    private fun getHarvestableSystemInRange(alreadyTried: Set<StarSystemAPI>, ly: Float): StarSystemAPI? {
        val loc: Vector2f = market?.locationInHyperspace ?: return null

        // What systems are in range?
        val targets: WeightedRandomPicker<StarSystemAPI> = WeightedRandomPicker<StarSystemAPI>()
        targets.add(market.starSystem)
        for (s in Global.getSector().starSystems) {
            if (Helper.isSpecialSystem(s)) continue
            if (alreadyTried.contains(s)) continue
            if (s.id == market.starSystem.id) continue
            if (s.hasTag(Tags.THEME_UNSAFE)) continue
            val a: Float = loc.getX() - s.hyperspaceAnchor.locationInHyperspace.getX()
            val b: Float = loc.getY() - s.hyperspaceAnchor.locationInHyperspace.getY()
            val c = a * a + b * b
            val lyDist = Global.getSettings().unitsPerLightYear
            val inRange = c <= lyDist * lyDist * ly * ly
            if (inRange) targets.add(s)
        }
        return if (targets.isEmpty) null else targets.pick()
    }

    fun spawnFleet(fleetType: String?): CampaignFleetAPI? {
        var combat = 0f
        var tanker = 0f
        var freighter = 0f
        when (fleetType) {
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

        // Get half the ships from Roider Union's choices
        val params = FleetParamsV3(
            market,
            null,  // loc in hyper; don't need if have market
            RoiderFactions.ROIDER_UNION,
            null,  // quality override
            fleetType,
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
        val fleet: CampaignFleetAPI? = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.isEmpty) return null


        // Get the other half from the market's faction
        val params2 = FleetParamsV3(
            market,
            null,  // loc in hyper; don't need if have market
            null,
            null,  // quality override
            fleetType,
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
        fleet.memoryWithoutUpdate.set(MINER_STARTING_FP, fleet.fleetPoints)
        Roider_Misc.sortFleetByShipSize(fleet)
        FleetFactoryV3.addCommanderAndOfficers(fleet, params, random)
        fleet.setFaction(market?.factionId, true)
        fleet.name = market?.faction?.getFleetTypeName(fleetType)
        //		fleet.setNoFactionInName(true);
        fleet.addEventListener(this)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_TRADE_FLEET, true)
        val postId = Ranks.POST_FLEET_COMMANDER
        var rankId = Ranks.SPACE_COMMANDER
        when (fleetType) {
            RoiderFleetTypes.MINER -> rankId = Ranks.CITIZEN
            RoiderFleetTypes.MINING_FLEET -> rankId = Ranks.SPACE_CAPTAIN
            RoiderFleetTypes.MINING_ARMADA -> rankId = Ranks.SPACE_ADMIRAL
        }
        fleet.commander.postId = postId
        fleet.commander.rankId = rankId
        market?.containingLocation?.addEntity(fleet)
        fleet.facing = Math.random().toFloat() * 360f
        fleet.setLocation(market?.primaryEntity?.location?.x ?: 0f,
            market?.primaryEntity?.location?.y ?: 0f)
        return fleet
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI,
                                                reason: CampaignEventListener.FleetDespawnReason, param: Any) {
        miners.remove(fleet)

//		if (!functional) return;
        if (reason == CampaignEventListener.FleetDespawnReason.REACHED_DESTINATION) {
            val spawnFP = fleet.memoryWithoutUpdate.get(MINER_STARTING_FP) as Int
            if (spawnFP > 0) {
                val fraction: Float = (fleet.fleetPoints / spawnFP).toFloat()
                returningMinerValue += fraction
            }
        }
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI, primaryWinner: CampaignFleetAPI, battle: BattleAPI) {}
}