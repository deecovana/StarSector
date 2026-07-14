package roiderUnion.skills.roaming

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.SkillsHelper
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class RingerMods : SCBaseSkillPlugin() {
    companion object {
        const val MANEUVER_BONUS = 30f
        const val MOVE_SLOW_BONUS = 3f

        private const val BONUS_TOKEN = "[BONUS]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_FLEET

    override fun addTooltip(p0: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.RINGER_MODS_MANEUVERABILITY.replace(BONUS_TOKEN, MANEUVER_BONUS.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.RINGER_MODS_SLOW_BURN.replace(BONUS_TOKEN, MOVE_SLOW_BONUS.toInt().toString()))
        
        tooltip?.addSpacer(10f)

        tooltip?.addPara(ExternalStrings.RINGER_MODS_SLOW_BURN_DESC, 0f, Misc.getGrayColor(), Misc.getHighlightColor())

    }

    override fun advance(data: SCData?, amount: Float?) {
        data?.fleet?.stats?.dynamic?.getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD)?.modifyFlat(id, MOVE_SLOW_BONUS, ExternalStrings.RINGER_MODS_SKILL_MOD_DESC)
        Helper.sector?.playerFleet?.stats?.accelerationMult?.modifyPercent(id, MANEUVER_BONUS, ExternalStrings.RINGER_MODS_SKILL_MOD_DESC)
    }

    override fun onActivation(data: SCData?) {
        data?.fleet?.stats?.dynamic?.getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD)?.modifyFlat(id, MOVE_SLOW_BONUS, ExternalStrings.RINGER_MODS_SKILL_MOD_DESC)
        Helper.sector?.playerFleet?.stats?.accelerationMult?.modifyPercent(id, MANEUVER_BONUS, ExternalStrings.RINGER_MODS_SKILL_MOD_DESC)
    }

    override fun onDeactivation(data: SCData?) {
        data?.fleet?.stats?.dynamic?.getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD)?.unmodify(id)
        Helper.sector?.playerFleet?.stats?.accelerationMult?.unmodify(id)
    }
}