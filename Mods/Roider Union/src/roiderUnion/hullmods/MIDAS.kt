package roiderUnion.hullmods
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.terrain.MagicAsteroidBeltTerrainPlugin
import org.magiclib.util.MagicIncompatibleHullmods
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.StringToken
import roiderUnion.ids.ShipsAndWings
import roiderUnion.cleanup.MadMIDASHealer
import roiderUnion.helpers.ExternalStrings.inParenthesis
import roiderUnion.helpers.ExternalStrings.replaceNumberToken
import roiderUnion.ids.hullmods.VHullmods
import java.awt.Color
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Author: SafariJohn
 */
open class MIDAS : BaseHullMod(), HullModFleetEffect {
    companion object {
        const val MOD_ID = "roider_MIDAS_impact"
        const val TOKEN_ARMOR = "\$primaryArmor"
        const val TOKEN_HULL = "\$primaryHull"
        const val TOKEN_ARMOR2 = "\$secondArmor"
        const val TOKEN_HULL2 = "\$secondHull"
        const val MAX_IMPACT_RESIST = 90f
        const val EMP_REDUCTION = 15f
        const val MASS_BONUS = 10f
        const val KINETIC_REDUCTION = 10f
        const val MODULE_ARMOR_MULT = 1.5f
        const val MODULE_HULL_MULT = 0.2f
        const val ARGOS_WING_COUNT = 3
        fun hasMIDASStatic(variant: ShipVariantAPI?): Boolean {
            if (variant == null) return false
            return (variant.hasHullMod(RoiderHullmods.MIDAS)
                    || variant.hasHullMod(RoiderHullmods.MIDAS_1)
                    || variant.hasHullMod(RoiderHullmods.MIDAS_2)
                    || variant.hasHullMod(RoiderHullmods.MIDAS_3))
        }
    }

    // Resist impacts from asteroid belts and fields on the campaign map
    // Heavily modified from MesoTroniK's TiandongRetrofit hullmod
    override fun advanceInCampaign(fleet: CampaignFleetAPI?) {
        if (Helper.anyNull(fleet)) return

        val fleetData: FleetDataAPI = fleet!!.fleetData
        var contributingSize = 0f
        var totalSize = 0f
        for (fleetMember in fleetData.membersListCopy) {
            // Lurking NoClassDefFoundError...
            // No idea what is going on, so just going to catch it and continue.
            try {
                val size: Float = when (fleetMember.hullSpec?.hullSize) {
                    ShipAPI.HullSize.FIGHTER, ShipAPI.HullSize.FRIGATE -> 1f
                    ShipAPI.HullSize.DESTROYER -> 2f
                    ShipAPI.HullSize.CRUISER -> 4f
                    ShipAPI.HullSize.CAPITAL_SHIP -> 8f
                    else -> 1f
                }
                totalSize += size
                if (hasMIDAS(fleetMember.variant)) {
                    contributingSize += size
                }
            } catch (er: NoClassDefFoundError) {
                Global.getLogger(MIDAS::class.java).error(er)
            }
        }
        val contribution = contributingSize / totalSize
        val magnitudeMult = 1f - min(contribution, MAX_IMPACT_RESIST / 100f)

        // MagicAsteroids time
        // Reduce chance of damaging impact
        fleet.commanderStats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DAMAGE_CHANCE)
            ?.modifyMult(MOD_ID, 1f - contribution)

        // Reduce pushing effect of impact
        fleet.commanderStats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_FORCE)
            ?.modifyMult(MOD_ID, magnitudeMult)
    }

    private fun hasMIDAS(variant: ShipVariantAPI?): Boolean {
        return hasMIDASStatic(variant)
    }

    override fun hasSModEffect(): Boolean = true

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(stats)) return

        if (isSMod(stats)) {
            stats!!.kineticDamageTakenMult.modifyMult(MOD_ID, 1f - KINETIC_REDUCTION / 100f)
//            stats.kineticShieldDamageTakenMult.modifyMult(MOD_ID, 1f - KINETIC_REDUCTION / 100f)
        }

        stats!!.empDamageTakenMult.modifyMult(MOD_ID, 1f - EMP_REDUCTION / 100f)

        // Reduce campaign asteroid impact damage
        stats.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DAMAGE)
            ?.modifyMult(id, 1f - MAX_IMPACT_RESIST / 100f)

        val variant = stats.variant ?: return
        if (variant.wings.any { it == ShipsAndWings.TRACKER_SINGLE || it == ShipsAndWings.TRACKER_WING }) {
            variant.addMod(RoiderHullmods.TRACKER_CORE_SHIP)
        } else {
            variant.removeMod(RoiderHullmods.TRACKER_CORE_SHIP)
        }

        swapBuiltInWings(variant, stats.numFighterBays?.modifiedInt ?: 0)

        if (!variant.hasHullMod(RoiderHullmods.MIDAS)) transferHullModsToModule(variant)
    }

    private fun swapBuiltInWings(variant: ShipVariantAPI, numFighterBays: Int) {
        if (variant.hasHullMod(HullMods.CONVERTED_BAY)) return
        val hullId = variant.hullSpec?.baseHullId ?: return
        if (TrackerSwap.WINGS_PER_SHIP.containsKey(hullId)) {
            val wingId = if (variant.hasHullMod(RoiderHullmods.TRACKER_SWAP)) ShipsAndWings.TRACKER_SINGLE
            else if (variant.hasHullMod(RoiderHullmods.GLITZ_SWITCH)) ShipsAndWings.GLITZ_WING
            else ShipsAndWings.BREAKER_WING
            var wingCount = TrackerSwap.WINGS_PER_SHIP[hullId] ?: 0
            if (numFighterBays < wingCount) wingCount = numFighterBays
            if (variant.wings.size < wingCount) wingCount = variant.wings.size
            for (i in 0 until wingCount) {
                variant.wings[i] = wingId
            }
        }
        if (hullId == ShipsAndWings.ARGOS
            && numFighterBays >= ARGOS_WING_COUNT
            && variant.wings.size >= ARGOS_WING_COUNT) {
            if (variant.hasHullMod(RoiderHullmods.TRACKER_SWAP)
                || variant.hasHullMod(RoiderHullmods.GLITZ_SWITCH)
            ) {
                variant.wings[1] = ShipsAndWings.BREAKER_WING
                variant.wings[2] = ShipsAndWings.BREAKER_WING
            } else {
                variant.wings[1] = ShipsAndWings.BORER_WING
                variant.wings[2] = ShipsAndWings.BORER_WING
            }
        }
    }

    private fun transferHullModsToModule(variant: ShipVariantAPI) {
        val mods = HashSet<String>()
        val sMods = HashSet<String>()
        for (mod in variant.nonBuiltInHullmods) {
//            if (!Roider_MIDAS_Armor.ALLOWED.contains(mod)) continue;
            if (ModuleMIDAS.BLOCKED.contains(mod)) continue
            mods.add(mod)
        }
        for (mod in variant.permaMods) {
//            if (!Roider_MIDAS_Armor.ALLOWED.contains(mod)) continue;
            if (ModuleMIDAS.BLOCKED.contains(mod)) continue
            val modSpec: HullModSpecAPI = Global.getSettings().getHullModSpec(mod)
            if (modSpec.hasTag(Tags.HULLMOD_DAMAGE)) mods.add(mod)
        }
        for (mod in variant.sMods) {
//            if (!Roider_MIDAS_Armor.ALLOWED.contains(mod)) continue;
            if (ModuleMIDAS.BLOCKED.contains(mod)) continue
            mods.add(mod)
            sMods.add(mod)
        }
        ModuleMIDAS.HULLMODS[variant.hullSpec.baseHullId] = mods
        ModuleMIDAS.S_HULLMODS[variant.hullSpec.baseHullId] = sMods
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (Helper.anyNull(ship?.variant)) return
        val variant = ship!!.variant

        ship.mass = ship.mass * (1f + MASS_BONUS / 100f)
        if (MadMIDASHealer.isMadMidas(ship.fleetMemberId) && !variant.permaMods.contains(RoiderHullmods.MIDAS)) {
            MadMIDASHealer.forceHealMIDAS(ship.variant)
        }

        if (GarruchaDraw.VALID_HULLS.contains(variant.hullSpec?.baseHullId) && variant.hasHullMod(VHullmods.CONVERTED_FIGHTER_BAY)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(
                variant,
                VHullmods.CONVERTED_FIGHTER_BAY, RoiderHullmods.GARRUCHA_DRAW
            )
        }
    }

    override fun getSModDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String {
        return Helper.floatToPercentString(KINETIC_REDUCTION)
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String {
        if (index == 0) return Helper.floatToPercentString(MAX_IMPACT_RESIST)
        if (index == 1) return Helper.floatToPercentString(EMP_REDUCTION)
        return if (index == 2) Helper.floatToPercentString(MASS_BONUS)
        else ExternalStrings.DEBUG_NULL
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI,
        hullSize: ShipAPI.HullSize,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        if (ship?.variant == null) return
        if (!hasMIDAS(ship.variant)) return
        if (ship.variant.hasHullMod(RoiderHullmods.MIDAS)) return
        if (ship.variant.hasHullMod(RoiderHullmods.MIDAS_ARMOR)) return
        val hullId = ship.hullSpec?.baseHullId ?: ""

        // Handle special armor and hull cases
        var armorMult = MODULE_ARMOR_MULT
        if (hullId == ShipsAndWings.TELAMON) armorMult = 1.6f
        var hullMult = MODULE_HULL_MULT
        if (hullId == ShipsAndWings.FIRESTORM) hullMult = 0.12f
        if (hullId == ShipsAndWings.TELAMON) hullMult = 0.15625f

        // Get armor bonus
        val armorBonus: StatBonus = ship.mutableStats?.armorBonus?.createCopy() ?: StatBonus()
        val baseArmor: Float = (ship.hullSpec?.armorRating ?: 1f) * armorMult
        val modArmor: Float = armorBonus.computeEffective(baseArmor)
        val armorHL: Color = if (modArmor > baseArmor) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()

        // Get hull bonus
        val hullBonus: StatBonus = ship.mutableStats?.hullBonus?.createCopy() ?: StatBonus()
        val baseHull: Float = (ship.hullSpec?.hitpoints ?: 1f) * hullMult
        val modHull: Float = hullBonus.computeEffective(baseHull)
        val hullHL: Color = if (modHull > baseHull) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()
        when (hullId) {
            ShipsAndWings.TELAMON -> {
                buildTelamonTooltip(ship, armorHL, hullHL, modArmor, baseArmor, modHull, baseHull, tooltip)
            }
            ShipsAndWings.FIRESTORM -> {
                val tokens = mutableListOf<StringToken>()
                tokens += getTokens(TOKEN_ARMOR, modArmor, baseArmor, armorHL)
                tokens += getTokens(TOKEN_HULL, modHull, baseHull, hullHL)
                val (hl, colors) = Helper.buildHighlightsLists(ExternalStrings.MIDAS_ARMOR_FIRESTORM, *tokens.toTypedArray())

                val armorReplace = if (modArmor != baseArmor) "%s %s" else "%s"
                val hullReplace = if (modHull != baseHull) "%s %s" else "%s"
                val text = ExternalStrings.MIDAS_ARMOR_FIRESTORM
                    .replace(TOKEN_ARMOR, armorReplace)
                    .replace(TOKEN_HULL, hullReplace)

                tooltip.addPara(text, Helper.PAD, colors.toTypedArray(), *hl.toTypedArray())
            }
            else -> {
                val tokens = mutableListOf<StringToken>()
                tokens += getTokens(TOKEN_ARMOR, modArmor, baseArmor, armorHL)
                tokens += getTokens(TOKEN_HULL, modHull, baseHull, hullHL)

                val (hl, colors) = Helper.buildHighlightsLists(ExternalStrings.MIDAS_ARMOR, *tokens.toTypedArray())

                val armorReplace = if (modArmor != baseArmor) "%s %s" else "%s"
                val hullReplace = if (modHull != baseHull) "%s %s" else "%s"
                val text = ExternalStrings.MIDAS_ARMOR
                    .replace(TOKEN_ARMOR, armorReplace)
                    .replace(TOKEN_HULL, hullReplace)

                tooltip.addPara(text, Helper.PAD, colors.toTypedArray(), *hl.toTypedArray())
            }
        }
    }

    private fun buildTelamonTooltip(
        ship: ShipAPI,
        armorHL: Color,
        hullHL: Color,
        modArmor: Float,
        baseArmor: Float,
        modHull: Float,
        baseHull: Float,
        tooltip: TooltipMakerAPI
    ) {
        val armorMult2 = 1.44f
        val hullMult2 = 0.09375f

        // Get second armor bonus
        val armorBonus2: StatBonus = ship.mutableStats?.armorBonus?.createCopy() ?: StatBonus()
        val baseArmor2: Float = (ship.hullSpec?.armorRating ?: 1f) * armorMult2
        val modArmor2: Float = armorBonus2.computeEffective(baseArmor2)

        // Get second hull bonus
        val hullBonus2: StatBonus = ship.mutableStats?.hullBonus?.createCopy() ?: StatBonus()
        val baseHull2: Float = (ship.hullSpec?.hitpoints ?: 1f) * hullMult2
        val modHull2: Float = hullBonus2.computeEffective(baseHull2)

        val tokens = mutableListOf<StringToken>()
        tokens += getTokens(TOKEN_ARMOR, modArmor, baseArmor, armorHL)
        tokens += getTokens(TOKEN_HULL, modHull, baseHull, hullHL)
        tokens += getTokens(TOKEN_ARMOR2, modArmor2, baseArmor2, armorHL)
        tokens += getTokens(TOKEN_HULL2, modHull2, baseHull2, hullHL)
        val (hl, colors) = Helper.buildHighlightsLists(ExternalStrings.MIDAS_ARMOR_TELAMON, *tokens.toTypedArray())

        val armorReplace = if (modArmor != baseArmor) "%s %s" else "%s"
        val hullReplace = if (modHull != baseHull) "%s %s" else "%s"
        val armor2Replace = if (modArmor2 != baseArmor2) "%s %s" else "%s"
        val hull2Replace = if (modHull2 != baseHull2) "%s %s" else "%s"
        val text = ExternalStrings.MIDAS_ARMOR_TELAMON
            .replace(TOKEN_ARMOR, armorReplace)
            .replace(TOKEN_HULL, hullReplace)
            .replace(TOKEN_ARMOR2, armor2Replace)
            .replace(TOKEN_HULL2, hull2Replace)

        tooltip.addPara(text, Helper.PAD, colors.toTypedArray(), *hl.toTypedArray())
    }

    private fun getTokens(token: String, modValue: Float, baseValue: Float, highlight: Color): List<StringToken> {
        val result = mutableListOf<StringToken>()
        result += StringToken(token, modValue.roundToInt().toString(), Misc.getHighlightColor())
        if (modValue != baseValue) result += StringToken(token, getModifierText(baseValue, modValue), highlight)
        return result
    }

    private fun getModifierText(baseValue: Float, modValue: Float): String {
        val intModified = (modValue - baseValue).roundToInt()
        return if (modValue > baseValue) {
            ExternalStrings.NUMBER_PLUS.replaceNumberToken(intModified).inParenthesis()
        } else {
            intModified.toString().inParenthesis()
        }
    }

    override fun isApplicableToShip(ship: ShipAPI?): Boolean {
        if (ship == null) return false
        if (MadMIDASHealer.isMadMidas(ship.fleetMemberId)) return false
        return if (ship.variant?.hasHullMod(RoiderHullmods.MIDAS) == true) true
        else !hasMIDAS(ship.variant)
    }

    override fun onFleetSync(fleet: CampaignFleetAPI) {}
    override fun withAdvanceInCampaign(): Boolean {
        return true
    }

    override fun withOnFleetSync(): Boolean {
        return false
    }
}