package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import roiderUnion.ids.RoiderIndustries

class Roider_UnionHQFunctional : BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI?>?
    ): Boolean {
        val market = dialog?.interactionTarget?.market ?: return false
        return market.hasFunctionalIndustry(RoiderIndustries.UNION_HQ)
    }
}