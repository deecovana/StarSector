package scripts.campaign.fleets.expeditions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.AddedEntity
import com.fs.starfarer.api.impl.campaign.tutorial.TutorialMissionIntel
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import org.lwjgl.util.vector.Vector2f
import roiderUnion.ids.*
import roiderUnion.ids.RoiderFleetTypes
import roiderUnion.ids.systems.AtkaIds
import scripts.Roider_Debug
import scripts.campaign.cleanup.Roider_ExpeditionLootCleaner
import roiderUnion.fleets.expeditionSpecials.PingTrapSpecial
import java.util.*

class Roider_TechExpeditionFleetRouteManager : BaseRouteFleetManager(6f, 14f) {
    class CustomData(system: StarSystemAPI, majorExpedition: Boolean) {
        val system: String
        val majorExpedition: Boolean

        init {
            this.system = system.id
            this.majorExpedition = majorExpedition
        }

        companion object {
            fun aliasAttributes(x: XStream) {
                x.aliasAttribute(CustomData::class.java, "system", "s")
                x.aliasAttribute(CustomData::class.java, "majorExpedition", "m")
            }
        }
    }

    private var currSystem // Changes each time a route is created
            : String
    private var delay: Float
    private val random: Random

    init {
        currSystem = pickTargetSystem().id
        delay = 1f
        if (Roider_Debug.TECH_EXPEDITIONS) delay = 0f
        random = Random()
    }

    override fun getRouteSourceId(): String {
        return "roider_expedition"
    }

    override fun getMaxFleets(): Int {
        // Limit by number of salvage routes in this system
        // and by this manager's total number of routes
        return if (Roider_Debug.TECH_EXPEDITIONS) 1
        else 0
    }

//		float salvage = getVeryApproximateSalvageValue(getCurrSystem());
//        int salvageFleets = (int) (1 + Math.min(salvage / 2, 7));

    // Limit by number of salvage routes in this system
    // and by this manager's total number of routes
//		return Math.min(max, salvageFleets);
    override fun advance(amount: Float) {
        val temp: Float = interval.elapsed
        if (Roider_Debug.TECH_EXPEDITIONS) interval.setInterval(1f, 1f) else interval.setInterval(1f, 14f)
        interval.elapsed = temp
        val days: Float = Misc.getDays(amount)
        //        days *= 100;

        // Delay before first major expedition can start
        if (!TutorialMissionIntel.isTutorialInProgress() && delay > 0) {
            delay -= days
        }
        interval.advance(days)
        if (interval.intervalElapsed()) {
            currSystem = pickTargetSystem().id
            if (Roider_Debug.TECH_EXPEDITIONS) currSystem = "Penelope's Star"
            val id = routeSourceId
            val max = maxFleets
            val maxTotal = 0
            val man = RouteManager.getInstance()

            // Check that this system's salvage routes don't surpass max
            val curr: Int = man.getNumRoutesFor(id)
            if (curr >= max) return

            // Check that this manager's routes across all systems don't surpass max
            var totalRoutes = 0
            for (loc in Global.getSector().allLocations) {
                for (route in man.getRoutesInLocation(loc)) {
                    if (route.spawner === this) totalRoutes++
                }
            }
            if (totalRoutes >= maxTotal) return
            addRouteFleetIfPossible()
        }
    }

    override fun addRouteFleetIfPossible() {
        if (TutorialMissionIntel.isTutorialInProgress()) {
            return
        }
        val market: MarketAPI = pickSourceMarket()
        val seed = Random().nextLong()
        val id = routeSourceId
        val extra = RouteManager.OptionalFleetData(market)
        val picker: WeightedRandomPicker<String> = WeightedRandomPicker<String>(random)
        picker.add(RoiderFleetTypes.EXPEDITION, 25f)
        picker.add(RoiderFleetTypes.MAJOR_EXPEDITION, getMajorExpeditionWeight(market))
        val type: String = picker.pick()
        val majorExpedition = type == RoiderFleetTypes.MAJOR_EXPEDITION
        val customData = CustomData(getCurrSystem(), majorExpedition)
        if (market.factionId == Factions.PLAYER) extra.factionId = Factions.INDEPENDENT
        if (majorExpedition) extra.factionId = RoiderFactions.ROIDER_UNION
        val route: RouteData = RouteManager.getInstance().addRoute(
            id,
            market, seed, extra, this, customData
        )
        val distLY: Float = Misc.getDistanceLY(market.locationInHyperspace, getCurrSystem().location)
        val travelDays = distLY * 1.5f
        val prepDays = 2f + Math.random().toFloat() * 3f
        val endDays = 8f + Math.random().toFloat() * 3f // longer since includes time from jump-point to source
        val totalTravelTime = prepDays + endDays + travelDays * 2f
        val stayDays = Math.max(20f, totalTravelTime)

        // Departure segments
        route.addSegment(RouteSegment(prepDays, market.primaryEntity))
        route.addSegment(RouteSegment(travelDays, market.primaryEntity, getCurrSystem().center))

        // Add loot stashes
        var minor = 1 + random.nextInt(3)
        var major = random.nextInt(2)
        val fake = 1 + random.nextInt(3)
        if (customData.majorExpedition) {
            if (random.nextBoolean()) minor++
            major++
        }
        val thiefId: String = Misc.genUID()
        val stashes: MutableList<SectorEntityToken> = ArrayList<SectorEntityToken>()
        // Minor stashes
        for (i in 0 until minor) {
            val stash: AddedEntity = createLootStash(
                getCurrSystem(), BaseThemeGenerator.pickHiddenLocationNotNearStar(
                    random, getCurrSystem(), 50f + random.nextFloat() * 100f, null
                )
            )

//            Roider_ExpeditionTrapCreator creator = new Roider_ExpeditionTrapCreator(random,
//                        0.9f, Roider_FleetTypes.MINING_FLEET,
//                        extra.factionId, market.getId(), 7, 14, false);
//
//            SpecialCreationContext context = new SalvageSpecialAssigner.SpecialCreationContext();
//
//            Object specialData = creator.createSpecial(stash.entity, context);
//            if (specialData != null) {
//                Misc.setSalvageSpecial(stash.entity, specialData);
//            }
            picker.clear()
//            picker.add("thiefTrap", 10f)
            picker.add("pingTrap", 10f)
            //            picker.add("droneDefenders", 1);
            val special: String = picker.pick()
            when (special) {
//                "thiefTrap" -> Misc.setSalvageSpecial(stash.entity,
//                    Roider_ThiefTrapSpecial.Roider_ThiefTrapSpecialData()
//                )
                "pingTrap" -> Misc.setSalvageSpecial(stash.entity, PingTrapSpecial.PingTrapSpecialData())
//                else -> Misc.setSalvageSpecial(stash.entity, Roider_ThiefTrapSpecial.Roider_ThiefTrapSpecialData())
            }
            stash.entity.addTag(Tags.EXPIRES)
            stash.entity.removeTag(Tags.NEUTRINO_LOW)
            stash.entity.removeTag(Tags.NEUTRINO)
            stash.entity.addTag(Tags.NEUTRINO_HIGH)

//            Misc.setSalvageSpecial(stash.entity, new Roider_ThiefTrapSpecialData());
            stash.entity.memoryWithoutUpdate.set(MemoryKeys.EXPEDITION_LOOT, true)
            //            stash.entity.setSensorProfile(1f);
            stash.entity.detectedRangeMod.unmodify()
            //            stash.entity.getDetectedRangeMod().modifyMult("roider_loot", 0.5f);
            stash.entity.memoryWithoutUpdate.set(MemoryKeys.THIEF_KEY, thiefId)
            stashes.add(stash.entity)
        }

        // Major stashes
        for (i in 0 until major) {
            stashes.add(
                createMajorLootStash(
                    totalTravelTime + stayDays,
                    route.factionId, market, thiefId
                )
            )
        }

        // Fake stashes
        for (i in 0 until fake) {
            val loc: EntityLocation = BaseThemeGenerator.pickHiddenLocationNotNearStar(
                random, getCurrSystem(), 50f + random.nextFloat() * 100f, null
            )
            var currLoc: Vector2f? = loc.location
            if (loc.orbit != null) currLoc = loc.orbit.computeCurrentLocation()
            if (currLoc == null) currLoc = Vector2f()
            val token: SectorEntityToken = getCurrSystem().createToken(currLoc)
            getCurrSystem().addEntity(token)
            if (loc.orbit != null) token.orbit = loc.orbit
            stashes.add(token)
        }
        val seg = RouteSegment(stayDays, getCurrSystem().center)
        seg.custom = stashes
        route.addSegment(seg)

        // Return segments
        route.addSegment(RouteSegment(travelDays, getCurrSystem().center, market.primaryEntity))
        route.addSegment(RouteSegment(endDays, market.primaryEntity))


        // Delay and intel
        var routeDelay = prepDays
        routeDelay *= 0.75f + Math.random().toFloat() * 0.5f
        routeDelay = routeDelay.toInt().toFloat()
        route.delay = routeDelay
        val intel: BaseIntelPlugin = Roider_TechExpeditionIntel(
            route, market.id,
            getCurrSystem().id, extra.factionId, thiefId
        )
        intel.setPostingRangeLY(1f, true)
        intel.postingLocation = market.primaryEntity
        Global.getSector().intelManager.queueIntel(intel, routeDelay + 3f)
        Global.getSector().addScript(intel)
    }

    private fun createMajorLootStash(
        duration: Float,
        faction: String, source: MarketAPI, thiefId: String
    ): SectorEntityToken {
        var faction = faction
        val stash: SectorEntityToken
        val id = "roider_stash_" + Misc.genUID()
        stash = getCurrSystem().addCustomEntity(id + "_major", null, RoiderIds.Entities.LOOT_STASH_MAJOR, null)
        stash.addTag(Tags.EXPIRES)

//        stash.getMemoryWithoutUpdate().set(Roider_MemFlags.EXPEDITION_LOOT, true);
        stash.getMemoryWithoutUpdate().set(MemoryKeys.EXPEDITION_LOOT_MAJOR, true)
        if (faction == Factions.INDEPENDENT) faction = Factions.PIRATES
        stash.getMemoryWithoutUpdate().set(MemoryKeys.EXPEDITION_FACTION, faction)
        stash.getMemoryWithoutUpdate().set(MemoryKeys.EXPEDITION_MARKET, source.id)
        stash.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, random.nextLong())
        stash.getMemoryWithoutUpdate().set(MemoryKeys.THIEF_KEY, thiefId)

        // Pick location
        val loc: EntityLocation = BaseThemeGenerator.pickHiddenLocationNotNearStar(
            random, getCurrSystem(), 50f + random.nextFloat() * 100f, null
        )
        var currLoc: Vector2f? = loc.location
        if (loc.orbit != null) currLoc = loc.orbit.computeCurrentLocation()
        if (currLoc == null) currLoc = Vector2f()
        val token: SectorEntityToken = getCurrSystem().createToken(currLoc)
        if (loc.orbit != null) token.orbit = loc.orbit
        getCurrSystem().addEntity(token)
        token.removeTag(Tags.HAS_INTERACTION_DIALOG)
        val zOrbit: OrbitAPI = Global.getFactory().createCircularOrbit(token, 0f, 0f, 1f)
        stash.setOrbit(zOrbit)
        getCurrSystem().addEntity(stash)

        // Save entity in token's data for later access
        // Have to avoid a ConcurrentModificationException
        token.customData.put(NomadStashPickupScript.STASH_ENTITY_KEY, stash)

//        String salvageSpecId = pickMajorSalvageSpec(stash);
//        stash.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, salvageSpecId);
        Global.getSector().addScript(Roider_ExpeditionLootCleaner(token, duration))
        return token
    }

    private fun createLootStash(system: StarSystemAPI, loc: EntityLocation): AddedEntity {
        val picker = WeightedRandomPicker<String>()
        picker.add(Entities.WRECK, 50f)
        picker.add(Entities.WEAPONS_CACHE, 4f)
        picker.add(Entities.WEAPONS_CACHE_SMALL, 10f)
        picker.add(Entities.WEAPONS_CACHE_HIGH, 4f)
        picker.add(Entities.WEAPONS_CACHE_SMALL_HIGH, 10f)
        picker.add(Entities.WEAPONS_CACHE_LOW, 4f)
        picker.add(Entities.WEAPONS_CACHE_SMALL_LOW, 10f)
        picker.add(Entities.SUPPLY_CACHE, 4f)
        picker.add(Entities.SUPPLY_CACHE_SMALL, 10f)
        picker.add(Entities.EQUIPMENT_CACHE, 4f)
        picker.add(Entities.EQUIPMENT_CACHE_SMALL, 10f)
        val type: String = picker.pick()
        if (type == Entities.WRECK) {
            val factions: List<String> = listOf(Factions.INDEPENDENT)
            picker.clear()
            picker.addAll(factions)
            var iter = 0
            do {
                if (iter > 110) break
                val faction: String = picker.pick()
                val params = DerelictShipEntityPlugin.createRandom(
                    faction,
                    null,
                    random,
                    DerelictShipEntityPlugin.getDefaultSModProb()
                )
                iter++
                if (params != null) {
                    if (params.ship.getVariant().hullSize == HullSize.CAPITAL_SHIP && iter < 100) {
                        continue
                    }
                    val wreck: CustomCampaignEntityAPI = BaseThemeGenerator.addSalvageEntity(
                        random,
                        system,
                        Entities.WRECK,
                        Factions.NEUTRAL,
                        params
                    ) as CustomCampaignEntityAPI
                    wreck.isDiscoverable = true
                    BaseThemeGenerator.setEntityLocation(
                        wreck,
                        loc, Entities.WRECK
                    )
                    return AddedEntity(
                        wreck,
                        null, Entities.WRECK
                    )
                }
            } while (true)
        }
        return BaseThemeGenerator.addEntity(
            random, system,
            loc, type, Factions.NEUTRAL
        )
    }

    fun getMajorExpeditionWeight(market: MarketAPI): Float {
        if (!market.hasIndustry(RoiderIndustries.UNION_HQ)) return 0f
        if (!market.getIndustry(RoiderIndustries.UNION_HQ).isFunctional) return 0f
        if (market.faction.isHostileTo(RoiderFactions.ROIDER_UNION)) return 0f
        return if (delay > 0) 0f
        else 5f
    }

    override fun spawnFleet(route: RouteData): CampaignFleetAPI? {
        val rRand: Random = route.random

        // Chance for indie expedition to secretly be pirates
        var pirate = false
        if (route.factionId == Factions.INDEPENDENT) pirate = rRand.nextBoolean()
        val data = route.custom as CustomData
        var type: String = RoiderFleetTypes.EXPEDITION
        if (data.majorExpedition) type = RoiderFleetTypes.MAJOR_EXPEDITION
        val system: StarSystemAPI = Global.getSector().getStarSystem(data.system)
        val fleet: CampaignFleetAPI = Roider_ExpeditionFleetFactory.createExpedition(
            type, system.location,
            route, route.market, pirate, rRand
        ) ?: return null
        fleet.isNoAutoDespawn = true
        fleet.addScript(Roider_TechExpeditionFleetAssignmentAI(fleet, route, pirate))

        // If fleet spawns while returning, then add random drops and mothballed ships
        if ((route.current.from === route.market || route.current.to === route.market)
            && route.currentIndex > 1
        ) {
            val limit = random.nextInt(4)
            for (i in 0 until limit) {
                NomadStashPickupScript.genMinorStashAndAdd(fleet, random)
            }
            if (data.majorExpedition) {
                NomadStashPickupScript.genMajorStashAndAdd(fleet, random)
            }
        }
        return fleet
    }

    private fun getCurrSystem(): StarSystemAPI {
        return Global.getSector().getStarSystem(currSystem)
    }

    override fun shouldCancelRouteAfterDelayCheck(data: RouteData): Boolean {
        return false
    }

    override fun shouldRepeat(route: RouteData): Boolean {
        return false
    }

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteData) {}

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_TechExpeditionFleetRouteManager::class.java, "currSystem", "s")
            x.aliasAttribute(Roider_TechExpeditionFleetRouteManager::class.java, "delay", "d")
            x.aliasAttribute(Roider_TechExpeditionFleetRouteManager::class.java, "random", "r")
        }

        //    private void initWreckPlugin(SectorEntityToken stash, DerelictShipData params) {
        //        float profile = stash.getSensorProfile();
        //        StatMod genProfile = stash.getDetectedRangeMod().getFlatBonus("gen");
        //
        //        DerelictShipEntityPlugin plugin = new DerelictShipEntityPlugin();
        //        plugin.init(stash, params);
        //
        //        stash.setDiscoveryXP(0f);
        //        stash.setSensorProfile(profile);
        //        if (genProfile == null) stash.getDetectedRangeMod().modifyFlat("gen", 0);
        //        else stash.getDetectedRangeMod().modifyFlat("gen", genProfile.getValue());
        //        stash.getMemoryWithoutUpdate().set(Roider_MemFlags.EXPEDITION_WRECK_PLUGIN, plugin);
        //    }
        fun getVeryApproximateSalvageValue(system: StarSystemAPI): Float {
            return system.getEntitiesWithTag(Tags.SALVAGEABLE).size.toFloat()
        }

        fun pickSourceMarket(): MarketAPI {
            if (Roider_Debug.TECH_EXPEDITIONS) return Global.getSector()
                .getStarSystem(AtkaIds.PRIMARY.name)
                .getEntityById(AtkaIds.KOROVIN.id).market
            val markets: WeightedRandomPicker<MarketAPI> = WeightedRandomPicker<MarketAPI>()
            for (market in Global.getSector().economy.marketsCopy) {
                var weight = 0f

//			if (market.isHidden()) continue;
                if (!market.hasSpaceport()) continue  // markets w/o spaceports don't launch fleets
                if (market.hasIndustry(RoiderIndustries.UNION_HQ)
                    && market.getIndustry(RoiderIndustries.UNION_HQ).isFunctional
                ) {
                    weight += (market.size * 2).toFloat()
                }
                if (market.hasIndustry(RoiderIndustries.DIVES)) {
                    weight += market.size.toFloat()
                }
                if (market.factionId == RoiderFactions.ROIDER_UNION) {
                    weight += (market.size / 2).toFloat()
                    weight *= 2f
                }
                markets.add(market, weight)
            }
            return markets.pick()
        }

        fun pickTargetSystem(): StarSystemAPI {
            val picker: WeightedRandomPicker<StarSystemAPI> = WeightedRandomPicker<StarSystemAPI>()
            for (system in Global.getSector().starSystems) {
                if (Global.getSector().playerFleet.containingLocation === system) continue
                if (system.hyperspaceAnchor == null) continue
                if (system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue
                if (system.jumpPoints.isEmpty()) continue
                if (!system.isProcgen) continue
                if (!system.hasTag(Tags.THEME_REMNANT)) continue
                if (system.hasTag(Tags.THEME_HIDDEN)) continue
                if (!Global.getSector().economy.getMarkets(system).isEmpty()) continue
                if (system.type != StarSystemGenerator.StarSystemType.NEBULA
                    && system.star == null
                ) continue
                picker.add(system, getVeryApproximateSalvageValue(system) + 1f)
            }
            var pick = picker.pick()
            if (pick != null) return pick


            // Try without picking remnant systems
            picker.clear()
            for (system in Global.getSector().starSystems) {
                if (Global.getSector().playerFleet.containingLocation === system) continue
                if (system.hyperspaceAnchor == null) continue
                if (system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) continue
                if (system.jumpPoints.isEmpty()) continue
                if (!system.isProcgen) continue
                if (system.hasTag(Tags.THEME_HIDDEN)) continue
                if (!Global.getSector().economy.getMarkets(system).isEmpty()) continue
                if (system.type != StarSystemGenerator.StarSystemType.NEBULA
                    && system.star == null
                ) continue
                picker.add(system, getVeryApproximateSalvageValue(system) + 1f)
            }
            pick = picker.pick()
            return pick ?: Global.getSector().starSystems[Random().nextInt(Global.getSector().starSystems.size)]

            // If this fails it is the player's fault.
        }
    }
}