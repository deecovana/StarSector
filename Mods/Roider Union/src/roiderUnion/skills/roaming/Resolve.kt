package roiderUnion.skills.roaming

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.SkillsHelper
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class Resolve : SCBaseSkillPlugin() {
    companion object {
        const val CR_BONUS = 0.05f
        const val REPAIR_SPEED = 0.8f
        const val DAMAGE_TAKEN = 0.7f
        const val CR_BONUS_DISPLAY = "5"
        const val REPAIR_SPEED_DISPLAY = "20"
        const val DAMAGE_TAKEN_DISPLAY = "30"

        private const val BONUS_TOKEN = "[BONUS]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_ALL_SHIPS

    override fun addTooltip(data: SCData, tooltip: TooltipMakerAPI) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.RESOLVE_CR_BONUS.replace(BONUS_TOKEN, CR_BONUS_DISPLAY))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.RESOLVE_REPAIR_BONUS.replace(BONUS_TOKEN, REPAIR_SPEED_DISPLAY))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.RESOLVE_DAMAGE_RESIST.replace(BONUS_TOKEN, DAMAGE_TAKEN_DISPLAY))

    }

    override fun applyEffectsBeforeShipCreation(data: SCData, stats: MutableShipStatsAPI?, variant: ShipVariantAPI, hullSize: ShipAPI.HullSize?, id: String?) {
        if (stats == null) return
        stats.weaponDamageTakenMult?.modifyMult(id, DAMAGE_TAKEN)
        stats.engineDamageTakenMult?.modifyMult(id, DAMAGE_TAKEN)

        stats.combatEngineRepairTimeMult?.modifyMult(id, REPAIR_SPEED)
        stats.combatWeaponRepairTimeMult?.modifyMult(id, REPAIR_SPEED)

        stats.maxCombatReadiness?.modifyFlat(id, CR_BONUS, ExternalStrings.RESOLVE_SKILL_MOD_DESC)
    }
}