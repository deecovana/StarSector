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

class Nomadic : SCBaseSkillPlugin() {
    companion object {
        const val STORAGE_PERCENT = 20f
        const val PERSONNEL_PERCENT = 50f
        const val FUEL_SUPPLY_MULT = 0.9f
        const val FUEL_SUPPLY_MULT_DISPLAY = 10f

        private const val BONUS_TOKEN = "[BONUS]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_FLEET

    override fun addTooltip(p0: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.NOMADIC_SKILL_CARGO.replace(BONUS_TOKEN, STORAGE_PERCENT.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.NOMADIC_SKILL_FUEL.replace(BONUS_TOKEN, STORAGE_PERCENT.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.NOMADIC_SKILL_PERSONNEL.replace(BONUS_TOKEN, PERSONNEL_PERCENT.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.NOMADIC_SKILL_FUEL_USAGE.replace(BONUS_TOKEN, FUEL_SUPPLY_MULT_DISPLAY.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.NOMADIC_SKILL_SUPPLY_USAGE.replace(BONUS_TOKEN, FUEL_SUPPLY_MULT_DISPLAY.toInt().toString()))
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
        stats?.maxCrewMod?.modifyPercent(id, PERSONNEL_PERCENT)

        stats?.fuelUseMod?.modifyMult(id, FUEL_SUPPLY_MULT)
        stats?.suppliesPerMonth?.modifyMult(id, FUEL_SUPPLY_MULT)
    }
}