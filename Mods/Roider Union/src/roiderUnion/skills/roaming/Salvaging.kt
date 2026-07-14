package roiderUnion.skills.roaming

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.SkillsHelper
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class Salvaging : SCBaseSkillPlugin() {
    companion object {
        const val RESOURCE_BONUS = 0.5f
        const val RESOURCE_DISPLAY = 50
        const val POST_BATTLE_BONUS = 0.2f
        const val POST_BATTLE_DISPLAY = 20
        const val CREW_LOSS_MULT = 0.5f
        const val CREW_LOSS_DISPLAY = 50

        private const val RESOURCE_TOKEN = "[RESOURCE BONUS]"
        private const val POST_BATTLE_TOKEN = "[POST BATTLE BONUS]"
        private const val CREW_LOSS_TOKEN = "[CREW LOSS BONUS]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_SALVAGING

    override fun addTooltip(data: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.SALVAGING_RESOURCES.replace(RESOURCE_TOKEN, RESOURCE_DISPLAY.toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.SALVAGING_POST_BATTLE.replace(POST_BATTLE_TOKEN, POST_BATTLE_DISPLAY.toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.SALVAGING_CREW_LOSS.replace(CREW_LOSS_TOKEN, CREW_LOSS_DISPLAY.toString()))
    }

    override fun advance(data: SCData?, amount: Float?) {
        data?.fleet?.stats?.dynamic?.getStat(Stats.NON_COMBAT_CREW_LOSS_MULT)?.modifyMult(id, CREW_LOSS_MULT)
        data?.fleet?.stats?.dynamic?.getStat(Stats.SALVAGE_VALUE_MULT_FLEET_NOT_RARE)?.modifyFlat(id, RESOURCE_BONUS)
        data?.fleet?.stats?.dynamic?.getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)?.modifyFlat(id, POST_BATTLE_BONUS)
    }

    override fun onActivation(data: SCData?) {
        data?.fleet?.stats?.dynamic?.getStat(Stats.NON_COMBAT_CREW_LOSS_MULT)?.modifyMult(id, CREW_LOSS_MULT)
        data?.fleet?.stats?.dynamic?.getStat(Stats.SALVAGE_VALUE_MULT_FLEET_NOT_RARE)?.modifyFlat(id, RESOURCE_BONUS)
        data?.fleet?.stats?.dynamic?.getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)?.modifyFlat(id, POST_BATTLE_BONUS)
    }

    override fun onDeactivation(data: SCData?) {
        data?.fleet?.stats?.dynamic?.getStat(Stats.NON_COMBAT_CREW_LOSS_MULT)?.unmodify(id)
        data?.fleet?.stats?.dynamic?.getStat(Stats.SALVAGE_VALUE_MULT_FLEET_NOT_RARE)?.unmodify(id)
        data?.fleet?.stats?.dynamic?.getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)?.unmodify(id)
    }

}