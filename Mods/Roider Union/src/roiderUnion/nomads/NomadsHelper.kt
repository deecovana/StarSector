package roiderUnion.nomads

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.*
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import retroLib.RetroLib_Tags
import retroLib.RetrofitsKeeper
import retroLib.impl.BaseRetrofitAdjuster
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.MarketHelper
import roiderUnion.ids.*
import roiderUnion.nomads.bases.NomadBaseIntelPlugin
import roiderUnion.nomads.bases.NomadBaseSpawnScript
import roiderUnion.retrofits.RoiderAllFilter
import java.util.*

object NomadsHelper {
    const val FAR_DIST = 36000f
    const val STATION_BASE_GAP = 200f

    val bases: MutableSet<SectorEntityToken>
        get() = Memory.get(MemoryKeys.NOMAD_BASES, { it is MutableSet<*> }, { mutableSetOf<SectorEntityToken>() }) as MutableSet<SectorEntityToken>

    val groups: MutableSet<NomadsData>
        get() = Memory.get(MemoryKeys.NOMAD_GROUPS, { it is MutableSet<*> }, { mutableSetOf<NomadsData>() }) as MutableSet<NomadsData>

    val activeGroups: MutableSet<NomadsData>
        get() = Memory.get(MemoryKeys.NOMAD_ACTIVE_GROUPS, { it is MutableSet<*> }, { mutableSetOf<NomadsData>() }) as MutableSet<NomadsData>

    fun createBaseIntel(base: SectorEntityToken) {
        val intel = NomadBaseIntelPlugin(base)
        base.memoryWithoutUpdate[MemoryKeys.NOMAD_BASE_INTEL] = intel
        Global.getSector().intelManager.addIntel(intel)
    }

    fun pickMigrationSource(): SectorEntityToken? {
        val sources = HashSet<SectorEntityToken>()
        sources.addAll(getMajorCoreMarkets())
        sources.addAll(bases.filter { it.memoryWithoutUpdate.getBoolean(MemoryKeys.NOMAD_BASE_ACTIVE) })
        val picker = WeightedRandomPicker<SectorEntityToken>(Helper.random)
        picker.addAll(sources)
        return picker.pick()
    }

    fun pickWanderersSource(location: SectorEntityToken?): SectorEntityToken? {
        if (location == null) return null
        val sources = HashSet<SectorEntityToken>()
        val nearby = Helper.sector!!.starSystems.filter {
            Misc.getDistanceLY(it.location, location.locationInHyperspace) < 5f
                    && !Helper.isPlayerInSystem(it)
        }
        sources.addAll(nearby.map { Helper.getTokenAtRandomSpot(it) })
        val picker = WeightedRandomPicker<SectorEntityToken>(Helper.random)
        picker.addAll(sources)
        return picker.pick()
    }

    fun getMajorCoreMarkets(): Collection<SectorEntityToken> {
        val results = HashSet<SectorEntityToken>()
        for (market in Helper.sector?.economy?.getMarketsInGroup(null) ?: listOf()) {
            if (market.hasFunctionalIndustry(RoiderIndustries.UNION_HQ)) {
                results.add(market.primaryEntity)
            }
            if (market.hasIndustry(RoiderIndustries.DIVES) && market.size >= 5) {
                results.add(market.primaryEntity)
            }
        }
        return results
    }

    fun getCoreMarkets(): Collection<SectorEntityToken> {
        val results = HashSet<SectorEntityToken>()
        for (market in Helper.sector?.economy?.getMarketsInGroup(null) ?: listOf()) {
            if (market.hasFunctionalIndustry(RoiderIndustries.UNION_HQ)) {
                results.add(market.primaryEntity)
            }
            if (market.hasIndustry(RoiderIndustries.DIVES)) {
                results.add(market.primaryEntity)
            }
        }
        return results
    }

    fun isNomadSystem(jumpPoint: SectorEntityToken, caresAboutTransponder: Boolean): Boolean {
        val jp = jumpPoint as? JumpPointAPI ?: return false
        val nomadBase = bases
            .firstOrNull { base -> jp.destinations.any { base.starSystem === it.destination.starSystem } } ?: return false
        val transponderCheck = !caresAboutTransponder || !nomadBase.faction.getCustomBoolean(Factions.CUSTOM_ALLOWS_TRANSPONDER_OFF_TRADE)
        return !nomadBase.isDiscoverable && transponderCheck
    }

    fun isNomadSystem(jumpPoint: SectorEntityToken): Boolean {
        return isNomadSystem(jumpPoint, false)
    }

    fun pickSystemForRoiderBase(): StarSystemAPI? {
        val random = Helper.random
        val far = WeightedRandomPicker<StarSystemAPI>(random)
        val picker = WeightedRandomPicker<StarSystemAPI>(random)
        for (system in Helper.sector?.starSystems ?: emptyList()) {
            val days = Helper.sector?.clock?.getElapsedDaysSince(system.lastPlayerVisitTimestamp) ?: 0f
            if (days < 30f) continue
            if (Helper.isSpecialSystem(system)) continue

            if (system.doNotShowIntelFromThisLocationOnMap == false) continue
            if (!system.isProcgen) continue
            if (isPlayerTooClose(system.location)) continue
            if (Memory.isFlag(PirateBaseManager.RECENTLY_USED_FOR_BASE, system?.center)) continue
            if (system.hasTag(Tags.THEME_CORE)) continue
            if (system.hasTag(Tags.THEME_UNSAFE)) continue
            if (system.hasTag(RoiderTags.THEME_SEEKER_PLAGUEBEARER)) continue

            var weight = 1f
            if (system.hasTag(Tags.THEME_MISC_SKIP)) weight += 1f
            if (system.hasTag(Tags.THEME_MISC)) weight += 3f
            if (system.hasTag(Tags.THEME_RUINS)) weight += 5f

            if (Misc.getMarketsInLocation(system).isNotEmpty()) continue

            val weights: LinkedHashMap<LocationType, Float> = LinkedHashMap()
            weights[LocationType.IN_ASTEROID_BELT] = 1f
            weights[LocationType.IN_ASTEROID_FIELD] = 1f
            weights[LocationType.IN_RING] = 1f
            weights[LocationType.PLANET_ORBIT] = 1f
            val locs = getLocations(random, system, HashSet(), STATION_BASE_GAP, weights)
            if (locs.isEmpty) continue

            val dist = system.location.length()
            val distMult = FAR_DIST / dist
            if (dist > FAR_DIST) {
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

    private fun isPlayerTooClose(locationInHyper: Vector2f): Boolean {
        val playerLoc = Helper.sector?.playerFleet?.locationInHyperspace ?: return false
        val dist = Misc.getDistanceLY(playerLoc, locationInHyper)
        return dist < RouteManager.SPAWN_DIST_LY
    }

    fun createBase(system: StarSystemAPI, faction: String): SectorEntityToken? {
        val loc = pickUncommonLocation(null, system, STATION_BASE_GAP, null) ?: return null
        val entity = Helper.createMakeshiftStation(system, loc, faction) ?: return null
        val name = NomadsNameKeeper.generateName(NomadsNameKeeper.Type.BASE)
        entity.name = name

        val market = Helper.factory?.createMarket(
            RoiderIds.Entities.NOMAD_MARKET_ID + Misc.genUID(),
            name,
            3
        ) ?: return null
        market.factionId = faction
        market.addCondition(Conditions.POPULATION_2)

        MarketHelper.setStorageMarket(market, entity)
        MarketHelper.setPrimaryEntity(market, entity)
        entity.sensorProfile = 1f
        entity.isDiscoverable = true
        entity.memoryWithoutUpdate[MemoryKeys.NOMAD_BASE] = true
        Helper.addStationSensorProfile(entity)
        entity.addTag(RoiderTags.NOMAD_BASE)
        market.reapplyIndustries()

//        entity.addScript(NomadBaseLevelTracker(entity))

        entity.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.ORBITAL_CONSTRUCTION)

        return entity
    }

    fun addBattlestation(
        system: StarSystemAPI, loc: EntityLocation,
        stationTypes: WeightedRandomPicker<String>
    ): CampaignFleetAPI? {
        val type = stationTypes.pick()
        val fleet = FleetFactoryV3.createEmptyFleet(Factions.REMNANTS, FleetTypes.BATTLESTATION, null) ?: return null
        val member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, type)
        fleet.fleetData.addFleetMember(member)

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE] = true
        fleet.addTag(Tags.NEUTRINO_HIGH)
        fleet.isStationMode = true
        system.addEntity(fleet)

        fleet.clearAbilities()
        Helper.addStationSensorProfile(fleet)
        fleet.ai = null
        setEntityLocation(fleet, loc, null)
        convertOrbitWithSpin(fleet, 5f)
        member.repairTracker.cr = member.repairTracker.maxCR

        return fleet
    }

    fun addOrbitingNomads(base: SectorEntityToken) {
        val radius = base.radius + 50f
        val minOrbitDays = radius / 20
        val maxOrbitDays = minOrbitDays + 10f

        base.containingLocation.addOrbitalJunk(
            base,
            RoiderIds.Entities.NOMAD_JUNK,
            Helper.random.nextInt(10) + 15,
            12f, 20f,
            radius,
            110f,
            minOrbitDays,
            maxOrbitDays,
            60f,
            120f
        )
    }

    fun removeOrbitingNomads(base: SectorEntityToken) {
        val nomadJunk = base.containingLocation.allEntities.filter { it.hasTag(RoiderIds.Entities.NOMAD_JUNK) }
        for (junk in nomadJunk) base.containingLocation.removeEntity(junk)
    }

    fun pickDestOrNearby(dest: SectorEntityToken): SectorEntityToken {
        return if (Helper.random.nextBoolean() && !dest.hasTag(RoiderTags.NOMAD_BASE)) {
            val picker = WeightedRandomPicker<SectorEntityToken>(Helper.random)
            picker.addAll(getCoreMarkets())
            picker.pick()
        } else {
            dest
        }
    }

    fun pickFaction(): String {
        if (bases.none { it.faction.id == Factions.INDEPENDENT }) return Factions.INDEPENDENT
        if (bases.none { it.faction.id == RoiderFactions.ROIDER_UNION }) return RoiderFactions.ROIDER_UNION

        val picker = WeightedRandomPicker<String>(Helper.random)
        NomadBaseSpawnScript.FACTIONS.forEach { picker.add(it.key, it.value) }
        if (picker.isEmpty) picker.add(Factions.INDEPENDENT)
        return picker.pick()
    }

    fun createNomads(): NomadsData {
        val result = NomadsData(NomadsNameKeeper.generateName(NomadsNameKeeper.Type.GROUP), pickDescription())
        result.knownBPs += pickRetrofits()
        groups += result
        return result
    }

    fun pickDescription(): String {
        val picker = WeightedRandomPicker<String>()
        picker.addAll(ExternalStrings.NOMAD_GROUP_DESCRIPTIONS)
        return picker.pick()
    }

    fun pickRetrofits(): Set<String> {
        val result = mutableSetOf<String>()
        val retrofits = RetrofitsKeeper.getRetrofits(RoiderAllFilter(BaseRetrofitAdjuster(null)))
        val argosRetrofits = retrofits.filter { it.tags.contains(Fitters.ARGOS) }
            .map { it.target }
            .distinct()
            .toMutableList()
        if (argosRetrofits.isNotEmpty()) {
            val originalSize = argosRetrofits.size
            var count = 0
            while (count < originalSize * NomadBaseSpawnScript.RETROFIT_STOCK_PERCENT) {
                count++
                val next = Helper.random.nextInt(argosRetrofits.size)
                result += argosRetrofits[next]
                argosRetrofits.removeAt(next)
            }
        }
        val nonHiddenRetrofits = retrofits.asSequence()
            .filter { it.tags.contains(RoiderFactions.ROIDER_UNION) }
            .filterNot { it.tags.contains(RetroLib_Tags.HIDDEN) }
            .map { it.target }
            .distinct()
            .toMutableList()
        if (nonHiddenRetrofits.isNotEmpty()) {
            val originalSize = nonHiddenRetrofits.size
            var count = 0
            while (count < originalSize * NomadBaseSpawnScript.RETROFIT_STOCK_PERCENT) {
                count++
                val next = Helper.random.nextInt(nonHiddenRetrofits.size)
                result += nonHiddenRetrofits[next]
                nonHiddenRetrofits.removeAt(next)
            }
        }
        val commonRetrofits = retrofits.asSequence()
            .filter { it.targetSpec.hasTag(RoiderTags.BASE_BP) }
            .map { it.target }
            .distinct()
            .toMutableList()
        result += commonRetrofits
        val frameRetrofits = retrofits.asSequence()
            .filter { retroLib.Helper.isFrameHull(it.targetSpec) }
            .map { it.target }
            .distinct()
            .toMutableList()
        result += frameRetrofits
        if (result.contains(ShipsAndWings.TRACKER_SINGLE)) result += ShipsAndWings.TRACKER_WING
        if (result.contains(ShipsAndWings.TRACKER_WING)) result += ShipsAndWings.TRACKER_SINGLE
        return result
    }

    fun pickInactiveOrNewNomadGroup(random: Random? = Helper.random): NomadsData {
        if (groups.isEmpty()) return createNomads()
        val picker = WeightedRandomPicker<NomadsData>(random)
        picker.addAll(groups.filterNot { activeGroups.contains(it) })
        picker.add(createNomads())
        val result = picker.pick()
        groups += result
        return result
    }

    fun pickNumNomadRoutes(nomadsLevel: NomadsLevel): Int {
        return 2 + nomadsLevel.ordinal
    }

    fun pickNumNomadWandererRoutes(numRoutes: Int): Int {
        return numRoutes * 2
    }
}