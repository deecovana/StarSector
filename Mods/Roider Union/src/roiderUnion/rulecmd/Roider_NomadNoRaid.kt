package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings

class Roider_NomadNoRaid : BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI?>?
    ): Boolean {
        dialog?.optionPanel?.setEnabled(MarketCMD.RAID, false)
        dialog?.optionPanel?.setTooltip(MarketCMD.RAID, ExternalStrings.NOMAD_NO_RAID)
        dialog?.optionPanel?.setEnabled(MarketCMD.BOMBARD, false)
        dialog?.optionPanel?.setTooltip(MarketCMD.BOMBARD, ExternalStrings.NOMAD_NO_BOMBARD)
        return true
    }
}