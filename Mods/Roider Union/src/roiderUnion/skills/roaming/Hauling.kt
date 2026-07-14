package roiderUnion.skills.roaming

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.SkillsHelper
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class Hauling : SCBaseSkillPlugin() {
    companion object {
        const val STORAGE_PERCENT = 30f
        const val BURN_BONUS = 1f

        private const val BONUS_TOKEN = "[BONUS]"
    }
    
    override fun getAffectsString(): String = ExternalStrings.SIC_FLEET

    override fun addTooltip(p0: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.NOMADIC_SKILL_CARGO.replace(BONUS_TOKEN, STORAGE_PERCENT.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.NOMADIC_SKILL_FUEL.replace(BONUS_TOKEN, STORAGE_PERCENT.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.HAULING_SKILL_BURN_BONUS.replace(BONUS_TOKEN, BURN_BONUS.toInt().toString()))
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData?,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI?,
        hullSize: ShipAPI.HullSize?,
        id: String?
    ) {
        stats?.cargoMod?.modifyPercent(id, STORAGE_PERCENT)
        stats?.fuelMod?.modifyPercent(id, STORAGE_PERCENT)
    }

    override fun advance(data: SCData, amount: Float) {
        data.fleet.stats.fleetwideMaxBurnMod.modifyFlat(id, BURN_BONUS, "Navigation Skill") // extern
    }

    override fun onActivation(data: SCData) {
        data.fleet.stats.fleetwideMaxBurnMod.modifyFlat(id, BURN_BONUS, "Navigation Skill")
    }

    override fun onDeactivation(data: SCData) {
        data.fleet.stats.fleetwideMaxBurnMod.unmodify(id)
    }
}