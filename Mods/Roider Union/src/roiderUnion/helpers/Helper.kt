package roiderUnion.helpers

import com.fs.starfarer.api.FactoryAPI
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.SettingsAPI
import com.fs.starfarer.api.SoundPlayerAPI
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.AllianceManager
import exerelin.campaign.PlayerFactionStore
import exerelin.utilities.NexUtilsFaction
import org.lazywizard.lazylib.ext.minus
import org.lwjgl.util.vector.Vector2f
import roiderUnion.ModPlugin
import roiderUnion.helpers.ExternalStrings.replaceNumberToken
import roiderUnion.ids.ModIds
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIndustries
import roiderUnion.ids.hullmods.RoiderHullmods
import java.awt.Color
import java.util.*
import kotlin.math.roundToInt

object Helper {
    const val PAD = 10f
    const val SMALL_PAD = 10f
    const val DIALOG_COLUMNS = 7
    const val DIALOG_ICON_SIZE = 58f

    const val REP_LEVEL_ORDINAL_OFFSET = 5

    val modId: String
        get() {
            return if (isModEnabled(ModIds.ROIDER_UNION_DEV)) ModIds.ROIDER_UNION_DEV
            else if (isModEnabled(ModIds.ROIDER_UNION)) ModIds.ROIDER_UNION
            else {
                settings?.modManager?.enabledModsCopy?.firstOrNull { it.id.startsWith(ModIds.ROIDER_UNION) }?.id
                    ?: ModIds.ROIDER_UNION
            }
    }

    fun hasCommission(faction: String?): Boolean {
        if (faction == null) return false
        if (ModPlugin.hasNexerelin) {
            if (AllianceManager.areFactionsAllied(PlayerFactionStore.getPlayerFactionId(), faction )) {
                return true
            }
            val commissionFaction = Misc.getCommissionFactionId()
            val allied = commissionFaction != null && AllianceManager.areFactionsAllied(commissionFaction, faction)
            if (allied) {
                return true
            }
        }
        return faction == Misc.getCommissionFactionId()
    }

    fun isModEnabled(id: String): Boolean {
        return settings?.modManager?.isModEnabled(id) == true
    }

    val roiders: FactionAPI?
        get() = sector?.getFaction(RoiderFactions.ROIDER_UNION)

    val random
        get() = Misc.random ?: Random()
    val isSectorPaused: Boolean
        get() = sector?.isPaused ?: true

    val sector: SectorAPI?
        get() = Global.getSector()

    val factory: FactoryAPI?
        get() = Global.getFactory()

    val combatEngine: CombatEngineAPI?
        get() = Global.getCombatEngine()

    val settings: SettingsAPI?
        get() = Global.getSettings()

    val soundPlayer: SoundPlayerAPI?
        get() = Global.getSoundPlayer()

    fun anyNull(vararg objects: Any?): Boolean {
        for (o in objects) {
            if (o == null) return true
        }

        return false
    }

    fun buildHighlightsLists(text: String, vararg tokens: StringToken): Pair<List<String>, List<Color>> {
        // Need to handle unplanned duplicate replaces
        for (hl in tokens) {
            hl.index = text.indexOf(hl.token)
        }

        val highlights = tokens.filter { it.index >= 0 }.sortedBy { it.index }.map { it.text }
        val colors = tokens.filter { it.index >= 0 }.sortedBy { it.index }.map { it.color }

        return Pair(highlights, colors)
    }

    fun toRepLevel(level: Int): RepLevel {
        return when(level) {
            4 -> RepLevel.COOPERATIVE
            3 -> RepLevel.FRIENDLY
            2 -> RepLevel.WELCOMING
            1 -> RepLevel.FAVORABLE
            0 -> RepLevel.NEUTRAL
            -1 -> RepLevel.SUSPICIOUS
            -2 -> RepLevel.INHOSPITABLE
            -3 -> RepLevel.HOSTILE
            -4 -> RepLevel.VENGEFUL
            else -> RepLevel.NEUTRAL
        }
    }

    fun toRepLevel(level: Float): RepLevel = toRepLevel((level + 0.01f).toInt())

    fun multToPercentString(mult: Float): String {
        return ExternalStrings.NUMBER_PERCENT.replaceNumberToken((mult * 100f).roundToInt())
    }

    fun floatToPercentString(float: Float): String {
        return ExternalStrings.NUMBER_PERCENT.replaceNumberToken(float.roundToInt())
    }

    fun percentReductionAsMult(reduction: Float): Float {
        return 1f - reduction * 0.01f
    }

    fun percentIncreaseAsMult(reduction: Float): Float {
        return 1f + reduction * 0.01f
    }

    fun setEntityLocation(entity: SectorEntityToken?, loc: Vector2f?) {
        if (anyNull(entity, loc)) return
        entity!!.setLocation(loc!!.x, loc.y)
    }

    fun addEntityToLocation(entity: CampaignFleetAPI?, location: LocationAPI?, position: Vector2f?) {
        if (anyNull(entity, location, position)) return

        setEntityLocation(entity, position)
        location!!.addEntity(entity)
    }

    fun setPopulation(market: MarketAPI, popSize: Int) {
        MarketHelper.setPopulation(market, popSize)
    }

    fun getNumberAround(mean: Float, percentVariance: Float): Float {
        val variance = mean * percentVariance
        return (mean - variance) + (random.nextFloat() * variance * 2f)
    }

    fun getTokenAtRandomSpot(it: StarSystemAPI): SectorEntityToken {
        var farthest = Vector2f()
        for (entity in it.allEntities) {
            val dist = Misc.getDistance(entity, it.center)
            if (dist > farthest.length()) farthest = entity.location
        }
        return it.createToken(random.nextFloat() * farthest.x, random.nextFloat() * farthest.y)
    }

    fun isSpecialSystem(system: StarSystemAPI): Boolean {
        if (system.hyperspaceAnchor?.locationInHyperspace == null) return true
        if (system.star == null && system.center == null) return true
        if (system.type != StarSystemGenerator.StarSystemType.NEBULA && system.star == null) return true
        if (system.hasPulsar()) return true
        if (system.hasTag(Tags.THEME_HIDDEN)) return true
        if (system.jumpPoints.isEmpty()) return true
        if (system.hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) return true

        return false
    }

    fun isPlayerInSystem(system: StarSystemAPI): Boolean {
        return sector?.playerFleet?.location === system
    }

    fun getRandomFloat(min: Float, max: Float, random: Random = Helper.random): Float {
        return random.nextFloat() * (max - min) + min
    }

    fun getBoolean(chance: Float, random: Random = Helper.random): Boolean {
        return random.nextFloat() < chance
    }

    fun roundToThousands(value: Double): Double {
        return ((value / 1000).roundToInt() * 1000).toDouble()
    }

    fun roundToHundreds(value: Double): Double {
        return ((value / 100).roundToInt() * 100).toDouble()
    }

    /**
     * Does not count S-mods as "built-in"
     * @param variant the ship variant to check
     * @return whether the variant has a MIDAS hullmod built-in
     */
    fun hasBuiltInMIDAS(variant: ShipVariantAPI): Boolean {
        return if (variant.hasHullMod(RoiderHullmods.MIDAS_1)
            || variant.hasHullMod(RoiderHullmods.MIDAS_2)
            || variant.hasHullMod(RoiderHullmods.MIDAS_3)
            || variant.hasHullMod(RoiderHullmods.MIDAS_ARMOR)
        ) {
            true
        } else {
            val notBuiltIn = (variant.sMods.contains(RoiderHullmods.MIDAS)
                    || variant.nonBuiltInHullmods.contains(RoiderHullmods.MIDAS))
            (variant.hasHullMod(RoiderHullmods.MIDAS) && !notBuiltIn)
        }
    }

    fun hasBuiltInMIDAS(member: FleetMemberAPI): Boolean {
        return hasBuiltInMIDAS(member.variant)
    }

    fun hasRoiders(market: MarketAPI?): Boolean {
        if (market == null) return false
        return market.hasIndustry(RoiderIndustries.DIVES)
                || market.hasIndustry(RoiderIndustries.UNION_HQ)
                || market.hasIndustry(RoiderIndustries.NOMAD_BASE)
    }

    fun addStationSensorProfile(entity: SectorEntityToken?) {
        entity?.detectedRangeMod?.modifyFlat("gen", 5000f)
    }

    fun createMakeshiftStation(system: StarSystemAPI, loc: EntityLocation, faction: String): SectorEntityToken? {
        val added = BaseThemeGenerator.addNonSalvageEntity(
            system,
            loc,
            Entities.MAKESHIFT_STATION,
            faction
        )
        if (added?.entity != null) BaseThemeGenerator.convertOrbitWithSpin(added.entity, -5f)
        return added?.entity
    }

    fun multHalfToFull(base: Float, random: Random = this.random): Float {
        return base * (0.5f + random.nextFloat() * 0.5f)
    }

    fun getAndJoinedCommodities(commodityIds: Collection<String>): String {
        val result = commodityIds.mapNotNull { settings?.getCommoditySpec(it)?.name }
        return Misc.getAndJoined(result).lowercase()
    }

    fun getOrbitCurrAngle(orbit: OrbitAPI?): Float {
        if (orbit == null) return 0f
        return Misc.getAngleInDegrees(orbit.computeCurrentLocation().minus(orbit.focus.location))
    }

    fun getOrbitRadius(orbit: OrbitAPI?): Float {
        if (orbit == null) return 0f
        return Misc.getDistance(orbit.focus.location, orbit.computeCurrentLocation())
    }

    fun setCircularOrbit(entity: SectorEntityToken?, orbit: OrbitAPI?) {
        if (anyNull(entity, orbit)) return
        entity!!.setCircularOrbit(orbit!!.focus, getOrbitCurrAngle(orbit), getOrbitRadius(orbit), orbit.orbitalPeriod)
    }

    fun getTravelDays(from: SectorEntityToken, to: SectorEntityToken): Float {
        return if (from.containingLocation !== to.containingLocation) {
            val dist = Misc.getDistance(from.locationInHyperspace, to.locationInHyperspace)
            dist / 1500f + RouteManager.IN_OUT_PHASE_DAYS * 2f
        } else {
            val dist = Misc.getDistance(from.location, to.location)
            dist / 1500f
        }
    }
}
