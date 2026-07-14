package roiderUnion.roidMining

import com.fs.starfarer.api.campaign.AsteroidAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidSource
import com.fs.starfarer.api.loading.WeaponSlotAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.campaign.CampaignUtils
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.helpers.MiningHelper
import roiderUnion.ids.RoiderTags
import java.util.*
import kotlin.math.roundToInt

object RoidMiningHelper {
    const val ASTEROID = "asteroid"
    const val ICE_ZONE = "iceZone"
    const val RING_ZONE = "ringZone"

    const val MAX_VALUE_MULT = 2.5f
    const val MIN_VALUE_MULT = 0.8f

    class ZoneData(
        val iceWeight: Float,
        val dustWeight: Float,
        val rockWeight: Float,
        val metalWeight: Float,
        val plutonWeight: Float
    ) {
        val total = iceWeight + dustWeight + rockWeight + metalWeight + plutonWeight
    }

    const val BASE_PROSPECT_RANGE = 1000f

    const val SEED_KEY = "\$roider_prospectingSeed"
    const val POINTS_KEY = "\$roider_prospectingPoints"
    const val TYPE_KEY = "\$roider_roidType"
    const val VALUE_KEY = "\$roider_roidValue"
    const val PERCENTS_KEY = "\$roider_roidTypeChances"
    const val ZONE_TYPE_KEY = "\$roider_zoneType"

    data class RoidRichness(
        val minRoids: Int,
        val maxRoids: Int,
        val iceMult: Float,
        val dustMult: Float,
        val rockMult: Float,
        val metalMult: Float,
        val plutonMult: Float,
    )

    const val DEPLETION_TAG = "roider_roidDepletionPoint"
    const val DEPLETION_MULT_KEY = "\$roider_roidDepletionMult"
    const val DEPLETION_MIN_DIST = 500f
    const val DEPLETION_MAX_DIST = 5000f

    const val FRIGATE_POWER_MULT = 1f
    const val DESTROYER_POWER_MULT = 0.8f
    const val CRUISER_POWER_MULT = 0.6f
    const val CAPITAL_POWER_MULT = 0.4f

    const val ID = "id"
    const val ICE = "ice"
    const val DUST = "dust"
    const val ROCK = "rock"
    const val METAL = "metal"
    const val PLUTON = "pluton"

    const val MIN_COVERAGE_PERCENT = 0.3f

    const val SMALL_SLOT_COVERAGE = 1f
    const val MEDIUM_SLOT_COVERAGE = 2f
    const val LARGE_SLOT_COVERAGE = 4f
    const val FIGHTER_BAY_COVERAGE = 6f

    var seed: Long
        get() = Memory.getNullable(SEED_KEY, Helper.sector?.playerFleet, { it is Long }, { 0L }) as? Long ?: 0L
        set(value) { Memory.set(SEED_KEY, value, Helper.sector?.playerFleet)}

    var points: Float
        get() = Memory.getNullable(POINTS_KEY, Helper.sector?.playerFleet, { it is Float }, { 0f }) as? Float ?: 0f
        set(value) { Memory.set(POINTS_KEY, value, Helper.sector?.playerFleet)}

    fun pollRoids(prospector: CampaignFleetAPI, prospectTarget: SectorEntityToken): RoidPollResult {
        val result = RoidPollResult()
        val sources = CampaignUtils.getNearbyEntitiesWithTag<SectorEntityToken>(
            prospectTarget,
            getProspectRange(prospector),
            RoiderTags.PROSPECTING_ZONE
        ) + prospectTarget
        sources.forEach {
            for (i in 0 until 7) {
                val random = Random(it.id.hashCode() + seed)
                val roidType = pickRoid(it, random)
                val value = roidType.baseValue * (random.nextFloat() * (MAX_VALUE_MULT - MIN_VALUE_MULT) + MIN_VALUE_MULT)
                result.addRoid(roidType, value)
            }
        }
        return result
    }

    private fun pickRoid(zone: SectorEntityToken?, random: Random): RoidType {
        if (zone == null) return RoidType.NULL
        val data = getZoneData(zone) ?: return RoidType.NULL
        val richness = getResourceRichness(zone.containingLocation)
        val picker = WeightedRandomPicker<RoidType>(random)
        picker.add(RoidType.DUST, data.dustWeight * averageRichMult(richness, RoidType.DUST))
        picker.add(RoidType.ROCK, data.rockWeight * averageRichMult(richness, RoidType.ROCK))
        picker.add(RoidType.ICE, data.iceWeight * averageRichMult(richness, RoidType.ICE))
        picker.add(RoidType.METAL, data.metalWeight * averageRichMult(richness, RoidType.METAL))
        picker.add(RoidType.PLUTON, data.plutonWeight * averageRichMult(richness, RoidType.PLUTON))
        picker.add(RoidType.NULL, (data.total * 2f) / 7f) // extern & stat
        return picker.pick()
    }

    fun getResourceRichness(location: LocationAPI): List<RoidRichness> {
        return MiningHelper.getMiningResourceConditions(location).mapNotNull { RoidMiningData.REZ_RICHES[it] }
    }

    private fun averageRichMult(richness: List<RoidRichness>, type: RoidType): Float {
        val mult = when (type) {
            RoidType.DUST -> richness.sumOf { it.dustMult.toDouble() }
            RoidType.ROCK -> richness.sumOf { it.rockMult.toDouble() }
            RoidType.ICE -> richness.sumOf { it.iceMult.toDouble() }
            RoidType.METAL -> richness.sumOf { it.metalMult.toDouble() }
            RoidType.PLUTON -> richness.sumOf { it.plutonMult.toDouble() }
            else -> 1.0
        }
        return (mult / richness.size.coerceAtLeast(1)).toFloat()
    }

    fun getZoneData(zone: SectorEntityToken): ZoneData? {
        val zoneType = Memory.getNullable(ZONE_TYPE_KEY, zone, { it is String }, { null }) as? String ?: return null
        return RoidMiningData.ZONE_DATA[zoneType]
    }

    fun getProspectRange(fleet: CampaignFleetAPI?): Float {
        if (fleet == null) return BASE_PROSPECT_RANGE
        // Impl stat for mod-modding
        return BASE_PROSPECT_RANGE
    }

    fun pollMinNumRoids(prospectTarget: SectorEntityToken): Int {
        val richness = getResourceRichness(prospectTarget.containingLocation)
        val min = richness.sumOf { it.minRoids } / richness.size.coerceAtLeast(1)
        return (min * getDepletionMult(prospectTarget)).roundToInt()
    }

    fun pollMaxNumRoids(prospectTarget: SectorEntityToken): Int {
        val richness = getResourceRichness(prospectTarget.containingLocation)
        val max = richness.sumOf { it.maxRoids } / richness.size.coerceAtLeast(1)
        return (max * getDepletionMult(prospectTarget)).roundToInt()
    }

    fun getDepletionMult(prospectTarget: SectorEntityToken): Double {
        val dps = getDepletionPoints(prospectTarget.containingLocation)
        if (dps.isEmpty()) return 1.0
        val dpMults = mutableListOf<Float>()
        for (dp in dps) {
            val baseMult = dp.customData[DEPLETION_MULT_KEY] as? Float ?: 1f
            val dist = Misc.getDistance(prospectTarget, dp)
            dpMults += baseMult + (1f - baseMult) * ((dist - DEPLETION_MIN_DIST) / DEPLETION_MAX_DIST).coerceIn(0f, 1f)
        }
        return dpMults.average()
    }

    fun isInTerrain(entity: SectorEntityToken?): Boolean {
        if (entity?.containingLocation == null) return false
        return entity.containingLocation.terrainCopy
            .filter { AsteroidBouncer.CIRCLE_TERRAINS.union(AsteroidBouncer.RING_TERRAINS).contains(it.plugin?.terrainId) }
            .any { it?.plugin?.containsPoint(entity.location, 1f) == true }
    }

    fun getDepletionPoints(location: LocationAPI): List<SectorEntityToken> {
        return location.getEntitiesWithTag(DEPLETION_TAG)
    }

    fun createRoid(random: Random, type: RoidType, containingLocation: LocationAPI?, source: AsteroidSource?): SectorEntityToken? {
        val size = random.nextFloat() * 3f + 2f
        val result = containingLocation?.addAsteroid(size) ?: return null
        result.setFaction(type.faction)
        Memory.set(TYPE_KEY, type, result)
        return result
    }

    fun getRoidType(roid: SectorEntityToken): RoidType? {
        return Memory.getNullable(TYPE_KEY, roid, { it is RoidType }, { null }) as? RoidType
    }

    fun getRoidValue(roid: SectorEntityToken): Float? {
        return Memory.getNullable(VALUE_KEY, roid, { it is Float }, { null }) as? Float
    }

    fun canMine(fleet: CampaignFleetAPI?, roid: SectorEntityToken): Boolean {
        if (fleet == null) return false
        //impl
        return true
    }

    fun tagAsteroids(sector: SectorAPI) {
        sector.starSystems?.forEach {
            it.allEntities?.filterIsInstance<AsteroidAPI>()?.forEach { asteroid ->
                if (asteroid.tags?.contains(RoiderTags.PROSPECTING_ZONE) == false) {
                    asteroid.addTag(RoiderTags.PROSPECTING_ZONE)
                    Memory.set(ZONE_TYPE_KEY, ASTEROID, asteroid)
                }
            }
        }
    }

    fun getMiningPower(fleet: CampaignFleetAPI, type: RoidType?): Float {
        var result = 0f
        fleet.fleetData?.membersListCopy?.forEach {
            result += getMiningPower(it, type)
        }
        return result
    }

    private fun getHullSizeMult(member: FleetMemberAPI): Float {
        if (true) return 1f
        return when (member.hullSpec?.hullSize) {
            ShipAPI.HullSize.FRIGATE -> FRIGATE_POWER_MULT
            ShipAPI.HullSize.DESTROYER -> DESTROYER_POWER_MULT
            ShipAPI.HullSize.CRUISER -> CRUISER_POWER_MULT
            ShipAPI.HullSize.CAPITAL_SHIP -> CAPITAL_POWER_MULT
            else -> 0f
        }
    }

    fun getMiningCoverageMult(member: FleetMemberAPI): Float {
        if (RoidMiningData.FULL_COVERAGE_HULLMODS.any { member.variant?.hasHullMod(it) == true }) return 1f
        val totalArea = getWeaponSlotsCoverageSize(member) + (member.stats?.numFighterBays?.modifiedValue ?: 0f) * FIGHTER_BAY_COVERAGE
        val miningWeaponCoverage = getMiningWeaponsCoverage(member) + getWingsCoverage(member)
        val specialCoverage = maxOf(RoidMiningData.SPECIAL_MINING_WEAPON_COVERAGE[member.hullId] ?: 0f,
            RoidMiningData.SPECIAL_MINING_WEAPON_COVERAGE[member.hullSpec?.baseHullId] ?: 0f)
        return ((miningWeaponCoverage + specialCoverage) / (totalArea * MIN_COVERAGE_PERCENT)).coerceAtMost(1f)
    }

    fun getWeaponSlotsCoverageSize(member: FleetMemberAPI): Float {
        val slots = member.hullSpec?.allWeaponSlotsCopy?.filterNot {
            it.isSystemSlot || it.isDecorative || it.isStationModule
        } ?: emptyList()
        return slots.map { getWeaponSizeCoverage(it.slotSize) }.sum()
    }

    fun getWeaponSizeCoverage(size: WeaponSize?): Float {
        return when (size) {
            WeaponSize.SMALL -> SMALL_SLOT_COVERAGE
            WeaponSize.MEDIUM -> MEDIUM_SLOT_COVERAGE
            WeaponSize.LARGE -> LARGE_SLOT_COVERAGE
            else -> 0f
        }
    }

    fun getMiningWeaponsCoverage(member: FleetMemberAPI): Float {
        val variant = member.variant ?: return 0f
        val weapons = variant.fittedWeaponSlots.map { variant.getWeaponId(it) }
        return weapons.filter { RoidMiningData.WEAPON_DATA.any { d -> d.id == it } }.map { getWeaponSizeCoverage(Helper.settings?.getWeaponSpec(it)?.size) }.sum()
    }

    fun getWingsCoverage(member: FleetMemberAPI): Float {
        val variant = member.variant ?: return 0f
        val numWings = variant.wings?.size ?: return 0f
        var numMiningWings = 0
        for (i in 0 until numWings) {
            val spec = variant.getWing(i) ?: continue
            val hasMiningWeapon = spec.variant?.hullSpec?.builtInWeapons?.any { RoidMiningData.WEAPON_DATA.any { d -> d.id == it.value } } == true
            if (hasMiningWeapon) numMiningWings++
        }
        return numMiningWings * FIGHTER_BAY_COVERAGE
    }

    fun getMiningPower(member: FleetMemberAPI, type: RoidType?): Float {
        return member.deploymentPointsCost * getMiningCoverageMult(member) * (getMiningEfficiency(member, type) + getMiningEfficiencyBonus(member, type))
    }

    fun getMiningEfficiency(member: FleetMemberAPI, type: RoidType?): Float {
        if (type == null || type == RoidType.NULL) return 1f
        val variant = member.variant ?: return 0f
        var result = if (RoidMiningData.FULL_COVERAGE_HULLMODS.any { member.variant?.hasHullMod(it) == true }) 0.5f // HACK extern
            else 0f
        for (slot in variant.fittedWeaponSlots ?: emptyList()) {
            val slotSpec = variant.getSlot(slot) ?: continue
            val weaponSpec = variant.getWeaponSpec(slot) ?: continue
            val weaponSlotTypeMatchMult = if (isWeaponSlotTypeMatch(slotSpec, weaponSpec)) 1f else 0.5f // extern
            val spec = RoidMiningData.WEAPON_DATA.firstOrNull { it.id == weaponSpec.weaponId } ?: continue
            val eff = when (type) {
                RoidType.ICE -> spec.iceEfficiency
                RoidType.DUST -> spec.dustEfficiency
                RoidType.ROCK -> spec.rockEfficiency
                RoidType.METAL -> spec.metalEfficiency
                RoidType.PLUTON -> spec.plutonEfficiency
                null -> 1f
                else -> 0f
            } * weaponSlotTypeMatchMult
            result = result.coerceAtLeast(eff)
        }
        return result
    }

    fun isWeaponSlotTypeMatch(slotSpec: WeaponSlotAPI, weaponSpec: WeaponSpecAPI): Boolean {
        val slotType = slotSpec.weaponType
        return when (weaponSpec.type) {
            WeaponAPI.WeaponType.BALLISTIC -> slotType != WeaponAPI.WeaponType.ENERGY && slotType != WeaponAPI.WeaponType.MISSILE
            WeaponAPI.WeaponType.ENERGY -> slotType != WeaponAPI.WeaponType.BALLISTIC && slotType != WeaponAPI.WeaponType.MISSILE
            WeaponAPI.WeaponType.MISSILE -> slotType != WeaponAPI.WeaponType.BALLISTIC && slotType != WeaponAPI.WeaponType.ENERGY
            else -> true
        }
    }

    fun getMiningEfficiencyBonus(member: FleetMemberAPI, type: RoidType?): Float {
        if (RoidMiningData.FULL_COVERAGE_HULLMODS.any { member.variant?.hasHullMod(it) == true }) return 0.5f // HACK extern
        return 0f
    }

    fun getMiningEfficiency(fleet: CampaignFleetAPI, type: RoidType?): Float {
        return fleet.fleetData.membersListCopy?.maxOf { getMiningEfficiency(it, type) } ?: 0f
    }
}