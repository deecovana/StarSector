package roiderUnion.skills.roaming

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.listeners.FleetMemberDeploymentListener
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.getRoundedValueMaxOneAfterDecimal
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.SkillsHelper
import roiderUnion.ids.RoiderIds
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin
import java.util.*

class Hulks : SCBaseSkillPlugin(), FleetMemberDeploymentListener {
    companion object {
        class DummyListener : FleetMemberDeploymentListener {
            override fun reportFleetMemberDeployed(member: DeployedFleetMemberAPI?) {}
        }

        const val MULT_TOKEN = "[MULT]"
        const val MULT_REDUCED_TOKEN = "[MULT REDUCED]"
        const val CUTOFF_TOKEN = "[CUTOFF]"
        const val SHIELD_MULT_TOKEN = "[SHIELD MULT]"
        const val SHIELD_MULT_REDUCED_TOKEN = "[SHIELD MULT REDUCED]"
        const val SHIELD_CUTOFF_TOKEN = "[SHIELD CUTOFF]"
        const val CARGO_MULT_TOKEN = "[CARGO CAP MULT]"
        const val FUEL_MULT_TOKEN = "[FUEL CAP MULT]"
        const val CREW_MULT_TOKEN = "[CREW CAP MULT]"

        val HULKS_FLAG_LISTENER = DummyListener()

        val ARMOR_CUTOFF = mapOf(
            Pair(HullSize.FRIGATE, 500f),
            Pair(HullSize.DESTROYER, 1000f),
            Pair(HullSize.CRUISER, 1200f),
            Pair(HullSize.CAPITAL_SHIP, 1500f)
        )
        const val ARMOR_CUTOFF_MULT = 0.1f
        val HULL_CUTOFF = mapOf(
            Pair(HullSize.FRIGATE, 1750f),
            Pair(HullSize.DESTROYER, 5000f),
            Pair(HullSize.CRUISER, 8000f),
            Pair(HullSize.CAPITAL_SHIP, 18000f)
        )
        const val HULL_CUTOFF_MULT = 0.1f
        val SHIELD_CUTOFF = mapOf(
            Pair(HullSize.FRIGATE, 5000f),
            Pair(HullSize.DESTROYER, 10000f),
            Pair(HullSize.CRUISER, 15000f),
            Pair(HullSize.CAPITAL_SHIP, 20000f)
        )
        const val SHIELD_CUTOFF_MULT = 0.1f

        const val FUEL_DIVISOR = 3f
        const val PERSONNEL_DIVISOR = 3f

        const val ARMOR_MULT = 1f
        const val HULL_MULT = 10f
        const val SHIELD_HP_MULT = 10f
        const val SHIELD_HP_MULT_FUEL = 0.5f
        const val SHIELD_HP_MULT_OTHER = 0.25f

        const val LOG_MOD_BONUS = 1f

        const val ID = RoiderIds.Skills.HULKS

        private const val BONUS_TOKEN = "[BONUS]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_ALL_SHIPS

    override fun addTooltip(data: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.HULKS_LOG_MODS.replace(BONUS_TOKEN, LOG_MOD_BONUS.toInt().toString()))
        tooltip?.addSpacer(Helper.SMALL_PAD)
        SkillsHelper.sicStandardTooltip(tooltip, getGeneralTooptip())
        SkillsHelper.sicStandardTooltip(tooltip, getShieldTooltip())
        SkillsHelper.sicStandardTooltip(tooltip, getArmorTooltip())
        SkillsHelper.sicStandardTooltip(tooltip, getHullTooltip())
        tooltip?.addSpacer(Helper.SMALL_PAD)
        tooltip?.addPara(ExternalStrings.HULKS_BUGGED, 0f, Misc.getGrayColor(), Misc.getHighlightColor())
        tooltip?.addSpacer(Helper.SMALL_PAD)
    }

    private fun getGeneralTooptip(): String {
        val cargoCapMult = Strings.X + "1"
        val fuelCapMult = Strings.X + "1/${FUEL_DIVISOR.toInt()}" // TODO extern?
        val crewCapMult = Strings.X + "1/${PERSONNEL_DIVISOR.toInt()}"
        return ExternalStrings.HULKS_GENERAL
            .replace(CARGO_MULT_TOKEN, cargoCapMult)
            .replace(FUEL_MULT_TOKEN, fuelCapMult)
            .replace(CREW_MULT_TOKEN, crewCapMult)
    }

    private fun getArmorTooltip(): String {
        val reducedMult = ARMOR_MULT * ARMOR_CUTOFF_MULT
        val actualShieldMult = SHIELD_HP_MULT * SHIELD_HP_MULT_OTHER
        val reducedShieldMult = actualShieldMult * SHIELD_CUTOFF_MULT
        val armorCutoffs = ARMOR_CUTOFF[HullSize.FRIGATE]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                ARMOR_CUTOFF[HullSize.DESTROYER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                ARMOR_CUTOFF[HullSize.CRUISER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                ARMOR_CUTOFF[HullSize.CAPITAL_SHIP]!!.toInt()
        val shieldCutoffs = SHIELD_CUTOFF[HullSize.FRIGATE]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.DESTROYER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.CRUISER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.CAPITAL_SHIP]!!.toInt()
        return ExternalStrings.HULKS_CREW
            .replace(MULT_TOKEN, Strings.X + ARMOR_MULT.toInt())
            .replace(MULT_REDUCED_TOKEN, Strings.X + reducedMult)
            .replace(CUTOFF_TOKEN, armorCutoffs)
            .replace(SHIELD_MULT_TOKEN, Strings.X + actualShieldMult.getRoundedValueMaxOneAfterDecimal())
            .replace(SHIELD_MULT_REDUCED_TOKEN, Strings.X + reducedShieldMult.getRoundedValueMaxOneAfterDecimal())
            .replace(SHIELD_CUTOFF_TOKEN, shieldCutoffs)
    }

    private fun getShieldTooltip(): String {
        val actualShieldMult = SHIELD_HP_MULT * SHIELD_HP_MULT_FUEL
        val reducedShieldMult = actualShieldMult * SHIELD_CUTOFF_MULT
        val shieldCutoffs = SHIELD_CUTOFF[HullSize.FRIGATE]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.DESTROYER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.CRUISER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.CAPITAL_SHIP]!!.toInt()
        return ExternalStrings.HULKS_FUEL
            .replace(SHIELD_MULT_TOKEN, Strings.X + actualShieldMult.getRoundedValueMaxOneAfterDecimal())
            .replace(SHIELD_MULT_REDUCED_TOKEN, Strings.X + reducedShieldMult.getRoundedValueMaxOneAfterDecimal())
            .replace(SHIELD_CUTOFF_TOKEN, shieldCutoffs)
    }

    private fun getHullTooltip(): String {
        val reducedMult = HULL_MULT * HULL_CUTOFF_MULT
        val actualShieldMult = SHIELD_HP_MULT * SHIELD_HP_MULT_OTHER
        val reducedShieldMult = actualShieldMult * SHIELD_CUTOFF_MULT
        val hullCutoffs = HULL_CUTOFF[HullSize.FRIGATE]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                HULL_CUTOFF[HullSize.DESTROYER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                HULL_CUTOFF[HullSize.CRUISER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                HULL_CUTOFF[HullSize.CAPITAL_SHIP]!!.toInt()
        val shieldCutoffs = SHIELD_CUTOFF[HullSize.FRIGATE]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.DESTROYER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.CRUISER]!!.toInt() + ExternalStrings.LIST_DIVIDER +
                SHIELD_CUTOFF[HullSize.CAPITAL_SHIP]!!.toInt()
        return ExternalStrings.HULKS_CARGO
            .replace(MULT_TOKEN, Strings.X + HULL_MULT.toInt())
            .replace(MULT_REDUCED_TOKEN, Strings.X + reducedMult.toInt())
            .replace(CUTOFF_TOKEN, hullCutoffs)
            .replace(SHIELD_MULT_TOKEN, Strings.X + actualShieldMult.getRoundedValueMaxOneAfterDecimal())
            .replace(SHIELD_MULT_REDUCED_TOKEN, Strings.X + reducedShieldMult.getRoundedValueMaxOneAfterDecimal())
            .replace(SHIELD_CUTOFF_TOKEN, shieldCutoffs)
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData?,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI?,
        hullSize: HullSize?,
        id: String?
    ) {
        if (stats == null) return
        stats.dynamic?.getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD)?.modifyFlat(id, LOG_MOD_BONUS)
        if (data?.isPlayer == true) stats.dynamic?.getMod(Stats.ACT_AS_COMBAT_SHIP)?.unmodify(this.id)
        if (hullSize == null) return
        val bonus = if (stats.fleetMember != null) getMemberBaseBonus(stats) else getCalculatedBaseBonus(stats)
        if (data?.isPlayer == true) updateStats(stats, hullSize, bonus)
        else updateNPCStats(stats, hullSize, bonus)
    }

    override fun applyEffectsAfterShipCreation(data: SCData?, ship: ShipAPI?, variant: ShipVariantAPI?, id: String?) {
        val stats = ship?.mutableStats ?: return
        val hullSize = ship.hullSize ?: return
        val bonus = if (stats.fleetMember != null) getMemberBaseBonus(stats) else getCalculatedBaseBonus(stats)
        if (data?.isPlayer == true) updateStats(stats, hullSize, bonus)
        else updateNPCStats(stats, hullSize, bonus)
    }

    private fun updateStats(stats: MutableShipStatsAPI, hullSize: HullSize, bonus: Float) {
        if (stats.hasListener(HULKS_FLAG_LISTENER)) stats.removeListener(HULKS_FLAG_LISTENER)
        var shield = 0f
        if (stats.variant?.hasHullMod(HullMods.ADDITIONAL_BERTHING) == true) {
            stats.armorBonus?.modifyFlat(ID, getArmorBonus(stats, hullSize, bonus * ARMOR_MULT))
            shield += SHIELD_HP_MULT_OTHER
        }
        if (stats.variant?.hasHullMod(HullMods.EXPANDED_CARGO_HOLDS) == true) {
            stats.hullBonus?.modifyFlat(ID, getHullBonus(stats, hullSize, bonus * HULL_MULT))
            shield += SHIELD_HP_MULT_OTHER
        }
        if (stats.variant?.hasHullMod(HullMods.AUXILIARY_FUEL_TANKS) == true) {
            shield += SHIELD_HP_MULT_FUEL
        }
        if (shield > 0) stats.shieldDamageTakenMult?.modifyMult(ID, getShieldEfficiencyMult(stats, hullSize, bonus * SHIELD_HP_MULT * shield))
    }

    private fun updateNPCStats(stats: MutableShipStatsAPI, hullSize: HullSize, bonus: Float) {
        if (bonus <= 0) return
        var buffed = false
        var shield = 0f
        val random = Random(stats.hashCode().toLong() + hullSize.hashCode().toLong())
        if (stats.variant?.hasHullMod(HullMods.ADDITIONAL_BERTHING) == true || random.nextFloat() < 0.166f) {
            buffed = true
            stats.armorBonus?.modifyFlat(ID, getArmorBonus(stats, hullSize, bonus * ARMOR_MULT))
            shield += SHIELD_HP_MULT_OTHER
        }
        if (stats.variant?.hasHullMod(HullMods.EXPANDED_CARGO_HOLDS) == true || random.nextFloat() < 0.333f) {
            buffed = true
            stats.hullBonus?.modifyFlat(ID, getHullBonus(stats, hullSize, bonus * HULL_MULT))
            shield += SHIELD_HP_MULT_OTHER
        }
        if (hasShield(stats) && (stats.variant?.hasHullMod(HullMods.AUXILIARY_FUEL_TANKS) == true || random.nextFloat() < 0.5f)) {
            buffed = true
            shield += SHIELD_HP_MULT_FUEL
        }
        if (shield > 0) stats.shieldDamageTakenMult?.modifyMult(ID, getShieldEfficiencyMult(stats, hullSize, bonus * SHIELD_HP_MULT * shield))
        if (buffed) {
            if (!stats.hasListener(HULKS_FLAG_LISTENER)) stats.addListener(HULKS_FLAG_LISTENER)
            stats.dynamic?.getMod(Stats.ACT_AS_COMBAT_SHIP)?.modifyFlat(id, 1f)
        }
    }

    private fun hasShield(stats: MutableShipStatsAPI): Boolean {
        return stats.variant?.hullSpec?.shieldType == ShieldAPI.ShieldType.FRONT || stats.variant?.hullSpec?.shieldType == ShieldAPI.ShieldType.OMNI
    }

    private fun getMemberBaseBonus(stats: MutableShipStatsAPI): Float {
        var result = stats.fleetMember?.cargoCapacity ?: 0f
        result += (stats.fleetMember?.fuelCapacity ?: 0f) / FUEL_DIVISOR
        result += (stats.fleetMember?.maxCrew ?: 0f) / PERSONNEL_DIVISOR
        return result
    }

    private fun getCalculatedBaseBonus(stats: MutableShipStatsAPI): Float {
        val hullSpec = stats.fleetMember?.hullSpec ?: stats.variant?.hullSpec
        var result = stats.cargoMod?.computeEffective(hullSpec?.cargo ?: 0f) ?: 0f
        result += (stats.fuelMod?.computeEffective(hullSpec?.fuel ?: 0f) ?: 0f) / FUEL_DIVISOR
        result += (stats.maxCrewMod?.computeEffective(hullSpec?.maxCrew ?: 0f) ?: 0f) / PERSONNEL_DIVISOR
        return result
    }

    private fun getArmorBonus(stats: MutableShipStatsAPI, hullSize: HullSize, bonus: Float): Float {
        val base = stats.variant?.hullSpec?.armorRating ?: 0f
        if (base + bonus <= (ARMOR_CUTOFF[hullSize] ?: 0f)) return bonus
        val over = ((base + bonus) - (ARMOR_CUTOFF[hullSize] ?: 0f)).coerceIn(0f, bonus)
        val overBonus = over * ARMOR_CUTOFF_MULT
        return (bonus - over) + overBonus
    }

    private fun getHullBonus(stats: MutableShipStatsAPI, hullSize: HullSize, bonus: Float): Float {
        val base = stats.variant?.hullSpec?.hitpoints ?: 0f
        if (base + bonus <= (HULL_CUTOFF[hullSize] ?: 0f)) return bonus
        val over = ((base + bonus) - (HULL_CUTOFF[hullSize] ?: 0f)).coerceIn(0f, bonus)
        val overBonus = over * HULL_CUTOFF_MULT
        return (bonus - over) + overBonus
    }

    private fun getShieldEfficiencyMult(stats: MutableShipStatsAPI, hullSize: HullSize, bonus: Float): Float {
        val eff = stats.variant?.hullSpec?.shieldSpec?.fluxPerDamageAbsorbed ?: 1f
        val caps = stats.fluxCapacity?.modified ?: 0f
        val base = caps / eff
        if (base + bonus <= (SHIELD_CUTOFF[hullSize] ?: 0f)) return base / (base + bonus)
        val over = ((base + bonus) - (SHIELD_CUTOFF[hullSize] ?: 0f)).coerceIn(0f, bonus)
        val overBonus = over * SHIELD_CUTOFF_MULT
        return base / (base + (bonus - over) + overBonus)
    }

    override fun reportFleetMemberDeployed(member: DeployedFleetMemberAPI?) {}
}