package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.getSourceMarket
import roiderUnion.nomads.NomadsHelper

class Roider_SetNomadBaseName : BaseCommandPlugin() {
    companion object {
        const val NOMAD_BASE_NAME_KEY = "\$roider_nomadBaseName"
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        val fleet = dialog?.interactionTarget as? CampaignFleetAPI ?: return false
        val source = NomadsHelper.bases.firstOrNull { it.market === fleet.getSourceMarket() } ?: return false
        memoryMap?.get(MemKeys.LOCAL)?.set(NOMAD_BASE_NAME_KEY, source.name)
        return true
    }
}