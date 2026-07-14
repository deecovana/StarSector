package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.hullmods.MIDAS

/**
 * Roider_ShowHullModDesc <id>
 * Author: SafariJohn
</id> */
class Roider_ShowHullModDesc : BaseCommandPlugin() {
    companion object {
        const val OPTION_MIDAS = "roider_midas"
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token>?,
        memoryMap: Map<String, MemoryAPI?>?
    ): Boolean {
        if (params.isNullOrEmpty()) return false
        val id = params[0].getString(memoryMap)
        val spec = Helper.settings?.getHullModSpec(id) ?: return false
        val tooltip = dialog?.textPanel?.beginTooltip() ?: return false
        val desc = tooltip.beginImageWithText(spec.spriteName, 32f)
        desc.addTitle(spec.displayName)
        tooltip.addImageWithText(Helper.PAD)
        when (id) {
            OPTION_MIDAS -> tooltip.addPara(
                spec.descriptionFormat,
                Helper.PAD,
                Misc.getHighlightColor(),
                Helper.floatToPercentString(MIDAS.MAX_IMPACT_RESIST),
                Helper.floatToPercentString(MIDAS.EMP_REDUCTION),
                Helper.floatToPercentString(MIDAS.MASS_BONUS)
            )

            else -> tooltip.addPara(spec.getDescription(HullSize.CRUISER), Helper.PAD)
        }
        dialog.textPanel?.addTooltip()
        return true
    }
}