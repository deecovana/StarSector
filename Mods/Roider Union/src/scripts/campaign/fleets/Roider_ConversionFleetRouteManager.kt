package scripts.campaign.fleets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.fleets.BaseRouteFleetManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.*
import scripts.campaign.fleets.expeditions.Roider_ExpeditionFleetFactory
import scripts.campaign.intel.Roider_ConversionFleetIntel
import java.util.*

/**
 * Author: SafariJohn
 */
class Roider_ConversionFleetRouteManager(minInterval: Float, maxInterval: Float) :
    BaseRouteFleetManager(minInterval, maxInterval), FleetEventListener {
    inner class Roider_ConvFleetData(val offerings: List<String>, loc: EntityLocation?) {
        val loc: EntityLocation?

        init {
            this.loc = loc
        }
    }

    override fun getMaxFleets(): Int {
        return 0
    }
    
    private val factions: Map<String, Float>
        get() = mapOf(Pair(Factions.INDEPENDENT, 1f))

    override fun addRouteFleetIfPossible() {
        // Recalculate faction weights
        val fWeights: MutableMap<String, Float> = HashMap()
        val factions = factions
        for (id in factions.keys) {
            var tally = 0f
            for (m in Global.getSector().economy.marketsCopy) {
                if (m.factionId == id && (m.hasIndustry(RoiderIndustries.DIVES)
                            || m.hasIndustry(RoiderIndustries.UNION_HQ))
                ) {
                    tally += 1f * factions[id]!!
                }
            }
            fWeights[id] = tally
        }

        // Pick faction
        val picker = WeightedRandomPicker<String>()
        for (id in fWeights.keys) {
            picker.add(id, fWeights[id] ?: 0f)
        }
        var factionId = picker.pick()
        if (factionId == null) factionId = Factions.PIRATES
        
        val marketPicker = WeightedRandomPicker<MarketAPI>()
        for (m in Global.getSector().economy.marketsCopy) {
            if (m.factionId == factionId) {
                var weight = m.shipQualityFactor
                if (m.hasIndustry(RoiderIndustries.UNION_HQ)) weight += weight else if (m.hasIndustry(
                        RoiderIndustries.DIVES)) weight += weight / 10f
                marketPicker.add(m, weight)
            }
        }
        
        val market = marketPicker.pick() ?: return


        // Pick location
        val system: StarSystemAPI = pickSystem()

//        if (true) system = Global.getSector().getStarSystem("Ounalashka");
        val lWeights: LinkedHashMap<LocationType, Float> = LinkedHashMap<LocationType, Float>()
        lWeights[LocationType.IN_ASTEROID_BELT] = 10f
        lWeights[LocationType.IN_ASTEROID_FIELD] = 10f
        lWeights[LocationType.IN_RING] = 10f
        lWeights[LocationType.IN_SMALL_NEBULA] = 10f
        lWeights[LocationType.GAS_GIANT_ORBIT] = 10f
        lWeights[LocationType.PLANET_ORBIT] = 10f
        val locs: WeightedRandomPicker<EntityLocation?> =
            BaseThemeGenerator.getLocations(null, system, null, 100f, lWeights)
        var loc: EntityLocation?
        do {
            if (locs.isEmpty) return
            loc = locs.pickAndRemove()
            if (loc == null) return

            // orbit focus must not be hostile to fleet faction
        } while (loc?.orbit != null && loc.orbit.focus.faction.isHostileTo(factionId))
        if (loc?.location == null) loc?.location = loc?.orbit?.focus?.location
        val random = Random()

        // Generate offerings
        val offerings: MutableList<String> = ArrayList()
        for (known in Global.getSector().getFaction(factionId).knownShips) {
            val spec = Global.getSettings().getHullSpec(known) ?: continue
            if (!spec.hasTag(RoiderTags.RETROFIT)) continue
            if (random.nextFloat() > OFFERINGS_CHANCE) continue
            offerings.add(known)
        }
        for (known in Global.getSector().getFaction(RoiderFactions.ROIDER_UNION).knownShips) {
            val spec = Global.getSettings().getHullSpec(known) ?: continue
            if (!spec.hasTag(RoiderTags.RETROFIT)) continue
            if (random.nextFloat() > OFFERINGS_CHANCE) continue
            offerings.add(known)
        }
        if (offerings.isEmpty()) offerings.add("roider_cyclops")
        val data = Roider_ConvFleetData(offerings, loc)
        val extra = RouteManager.OptionalFleetData()
        extra.factionId = factionId
        extra.fleetType = RoiderFleetTypes.MOTHER_EXPEDITION
        val route: RouteManager.RouteData =
            RouteManager.getInstance().addRoute(routeSourceId, market, Random().nextLong(), extra, this, data)
        route.addSegment(RouteManager.RouteSegment(60f, system.createToken(loc?.location)))
    }

    private fun pickSystem(): StarSystemAPI {
        val random = Random()
        val far: WeightedRandomPicker<StarSystemAPI> = WeightedRandomPicker<StarSystemAPI>(random)
        val picker: WeightedRandomPicker<StarSystemAPI> = WeightedRandomPicker<StarSystemAPI>(random)
        for (system in Global.getSector().starSystems) {
            val days = Global.getSector().clock.getElapsedDaysSince(system.lastPlayerVisitTimestamp)
            if (days < 45f) continue
            var weight = 0f
            if (system.hasTag(Tags.THEME_MISC_SKIP)) {
                weight = 2f
            } else if (system.hasTag(Tags.THEME_MISC)) {
                weight = 6f
            } else if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS)) {
                weight = 6f
            } else if (system.hasTag(Tags.THEME_RUINS)) {
                weight = 10f
            } else if (system.hasTag(Tags.THEME_CORE_UNPOPULATED)) {
                weight = 1f
            } else if (system.hasTag(Tags.THEME_CORE_POPULATED)) {
                weight = 1f
            }
            if (weight <= 0f) continue
            if (Misc.hasPulsar(system)) continue
            val dist = system.location.length()


//			float distMult = 1f - dist / 20000f;
//			if (distMult > 1f) distMult = 1f;
//			if (distMult < 0.1f) distMult = 0.1f;
            val distMult = 1f
            if (dist > 36000f) {
                far.add(system, weight * distMult)
            } else {
                picker.add(system, weight * distMult)
            }
        }
        if (picker.isEmpty) {
            picker.addAll(far)
        }
        return picker.pick()
    }

    override fun spawnFleet(route: RouteManager.RouteData): CampaignFleetAPI? {
        val data = route.custom as Roider_ConvFleetData
        val system: StarSystemAPI = data.loc?.orbit?.focus?.containingLocation as StarSystemAPI
        val random = Random(route.seed)
        val fleet: CampaignFleetAPI = Roider_ExpeditionFleetFactory.createExpedition(
            route.extra.fleetType, system.location,
            route, route.market, false, random
        ) ?: return null
        if (!hasAnArgos(fleet)) {
            val argosVariants: MutableList<String> = ArrayList()
            argosVariants.add("roider_argos_Outdated")
            argosVariants.add("roider_argos_Balanced")
            argosVariants.add("roider_argos_Support")
            val variant = argosVariants[random.nextInt(argosVariants.size)]
            val argos: FleetMemberAPI = fleet.fleetData.addFleetMember(variant)
            argos.repairTracker.cr = 0.7f
            val fFlag: FleetMemberAPI = fleet.flagship
            // Move captain
            argos.captain = fFlag.captain
            fFlag.captain = null
            // Change flagship
            fFlag.isFlagship = false
            argos.isFlagship = true
            fleet.fleetData.setFlagship(argos)
            fleet.fleetData.sort()
            fleet.fleetData.setSyncNeeded()
        }
        fleet.clearAssignments()
        val token: SectorEntityToken = route.current.from
        token.orbit = data.loc.orbit
        fleet.setLocation(token.location.x, token.location.y)
        system.addEntity(fleet)
        var daysRemaining: Float = route.current.daysMax
        daysRemaining -= route.current.elapsed
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, token, daysRemaining, "offering retrofit services")
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, token, Short.MAX_VALUE.toFloat(),
            "Dispersing") // extern
        val fleetMemory: MemoryAPI = fleet.memoryWithoutUpdate
        fleetMemory.set(MemoryKeys.APR_OFFERINGS, data.offerings)
        fleetMemory.set(MemoryKeys.APR_RETROFITTING, true)
        Misc.setFlagWithReason(
            fleetMemory, MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,
            routeSourceId, true, Short.MAX_VALUE.toFloat()
        )
        Misc.setFlagWithReason(
            fleetMemory, MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE,
            routeSourceId, true, Short.MAX_VALUE.toFloat()
        )
        if (!fleet.faction.getCustomBoolean(Factions.CUSTOM_OFFERS_COMMISSIONS)) {
            fleetMemory.set(MemoryKeys.APR_IGNORE_COM, true)
        }
        if (fleet.faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)) {
            fleetMemory.set(MemoryKeys.APR_IGNORE_REP, true)
        }
        if (fleet.faction.getCustomBoolean(Factions.CUSTOM_ALLOWS_TRANSPONDER_OFF_TRADE)) {
            fleetMemory.set(MemoryKeys.APR_IGNORE_TRANSPONDER, true)
        }

        fleet.isTransponderOn = !fleet.faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)
        fleet.removeAbility(Abilities.TRANSPONDER)
        val intel = Roider_ConversionFleetIntel(fleet, daysRemaining)
        //        Global.getSector().getIntelManager().addIntel(intel, true);
        Global.getSector().addScript(intel)
        fleet.addEventListener(this)
        fleet.addEventListener(intel)
        return fleet
    }

    private fun hasAnArgos(fleet: CampaignFleetAPI): Boolean {
        for (m in fleet.membersWithFightersCopy) {
            if (m.hullId.contains("roider_argos")) return true
        }
        return false
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI, primaryWinner: CampaignFleetAPI, battle: BattleAPI) {
        // If player and fleet on opposite sides, no longer offer retrofit services
        if (battle.isPlayerInvolved && !battle.onPlayerSide(fleet)) {
//            fleet.getMemoryWithoutUpdate().unset(Roider_MemFlags.APR_OFFERINGS);
            fleet.memoryWithoutUpdate.unset(MemoryKeys.APR_RETROFITTING)
            //            Misc.setFlagWithReason(fleet.getMemoryWithoutUpdate(), MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,
//                        ROUTE_ID, false, Short.MAX_VALUE);
//            Misc.setFlagWithReason(fleet.getMemoryWithoutUpdate(), MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE,
//                        ROUTE_ID, false, Short.MAX_VALUE);
        }

        // If lost all Argosi, disperse
        var hasArgos = false
        for (m in fleet.membersWithFightersCopy) {
            if (m.hullId.startsWith(ShipsAndWings.ARGOS)) {
                hasArgos = true
                break
            }
        }
        if (!hasArgos) {
            val token: SectorEntityToken = fleet.containingLocation.createToken(fleet.location)
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, token, Short.MAX_VALUE.toFloat(),
                "Dispersing") // extern
        }
    }

    override fun shouldCancelRouteAfterDelayCheck(route: RouteManager.RouteData): Boolean {
        return false
    }

    override fun shouldRepeat(route: RouteManager.RouteData): Boolean {
        return false
    }

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteManager.RouteData) {}
    override fun getRouteSourceId(): String {
        return ROUTE_SOURCE_ID
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI, reason: CampaignEventListener.FleetDespawnReason, param: Any) {}

    companion object {
        fun aliasAttributes(x: XStream) {
            x.alias("roider_cfdata", Roider_ConvFleetData::class.java)
            x.aliasAttribute(Roider_ConvFleetData::class.java, "offerings", "o")
            x.aliasAttribute(Roider_ConvFleetData::class.java, "loc", "l")
        }

        const val ROUTE_SOURCE_ID = "roider_conversionFleets"
        const val OFFERINGS_CHANCE = 0.8f
    }
}