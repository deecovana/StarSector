package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 */
class Roider_HasModId : BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token>?,
        memoryMap: Map<String, MemoryAPI?>?
    ): Boolean {
        if (params.isNullOrEmpty()) return false
        val modId = params[0].getString(memoryMap) ?: return false
        return Helper.isModEnabled(modId)
    }
}