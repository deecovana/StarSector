package roiderUnion.skills.roaming

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.SkillsHelper
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class RockpiperEngineers : SCBaseSkillPlugin() {
    companion object {
        const val INSTA_REPAIR_FRACTION = 0.3f
        const val INSTA_REPAIR_DISPLAY = 30
        val D_MOD_AVOID_CHANCE = mapOf(
            Pair(HullSize.FRIGATE, 0.4f),
            Pair(HullSize.DESTROYER, 0.4f),
            Pair(HullSize.CRUISER, 0.6f),
            Pair(HullSize.CAPITAL_SHIP, 0.7f)
        )
        val D_MOD_AVOID_DISPLAY = mapOf(
            Pair(HullSize.FRIGATE, 60),
            Pair(HullSize.DESTROYER, 60),
            Pair(HullSize.CRUISER, 40),
            Pair(HullSize.CAPITAL_SHIP, 30)
        )
        const val MIN_RECOVERY_HULL = 0.3f
        const val MAX_RECOVERY_HULL = 0.4f
        const val MIN_RECOVERY_HULL_DISPLAY = 30
        const val MAX_RECOVERY_HULL_DISPLAY = 40

        private const val D_MODS_TOKEN = "[CHANCES]"
        private const val INSTA_REPAIR_TOKEN = "[INSTA REPAIR]"
        private const val MIN_HULL_TOKEN = "[MIN HULL]"
        private const val MAX_HULL_TOKEN = "[MAX HULL]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_ALL_SHIPS

    override fun addTooltip(p0: SCData?, tooltip: TooltipMakerAPI?) {
        val chances = D_MOD_AVOID_DISPLAY[HullSize.FRIGATE]?.toString() + ExternalStrings.LIST_DIVIDER +
                D_MOD_AVOID_DISPLAY[HullSize.DESTROYER]?.toString() + ExternalStrings.LIST_DIVIDER +
                D_MOD_AVOID_DISPLAY[HullSize.CRUISER]?.toString() + ExternalStrings.LIST_DIVIDER +
                D_MOD_AVOID_DISPLAY[HullSize.CAPITAL_SHIP]?.toString()
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.ROCKPIPER_ENGINEERS_D_MODS.replace(D_MODS_TOKEN, chances))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.ROCKPIPER_ENGINEERS_INSTA_REPAIR.replace(INSTA_REPAIR_TOKEN, INSTA_REPAIR_DISPLAY.toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.ROCKPIPER_ENGINEERS_HULL_RECOVERY
            .replace(MIN_HULL_TOKEN, MIN_RECOVERY_HULL_DISPLAY.toString())
            .replace(MAX_HULL_TOKEN, MAX_RECOVERY_HULL_DISPLAY.toString()))
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData?,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI?,
        hullSize: HullSize?,
        id: String?
    ) {
        stats?.dynamic?.getMod(Stats.INSTA_REPAIR_FRACTION)?.modifyFlat(id, INSTA_REPAIR_FRACTION)
        val dModChanceMod = D_MOD_AVOID_CHANCE[hullSize] ?: return
        stats?.dynamic?.getMod(Stats.DMOD_ACQUIRE_PROB_MOD)?.modifyMult(id, dModChanceMod)
    }

    override fun advance(data: SCData?, amunt: Float?) {
        data?.fleet?.stats?.dynamic?.getMod(Stats.RECOVERED_HULL_MIN)?.modifyFlat(id, MIN_RECOVERY_HULL)
        data?.fleet?.stats?.dynamic?.getMod(Stats.RECOVERED_HULL_MAX)?.modifyFlat(id, MAX_RECOVERY_HULL)
    }

    override fun onActivation(data: SCData?) {
        data?.fleet?.stats?.dynamic?.getMod(Stats.RECOVERED_HULL_MIN)?.modifyFlat(id, MIN_RECOVERY_HULL)
        data?.fleet?.stats?.dynamic?.getMod(Stats.RECOVERED_HULL_MAX)?.modifyFlat(id, MAX_RECOVERY_HULL)
    }

    override fun onDeactivation(data: SCData?) {
        data?.fleet?.stats?.dynamic?.getMod(Stats.RECOVERED_HULL_MIN)?.unmodify(id)
        data?.fleet?.stats?.dynamic?.getMod(Stats.RECOVERED_HULL_MAX)?.unmodify(id)
    }
}