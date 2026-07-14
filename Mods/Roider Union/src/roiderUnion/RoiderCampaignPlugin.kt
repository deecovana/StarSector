package roiderUnion

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import roiderUnion.helpers.Memory
import roiderUnion.ids.ModIds
import roiderUnion.ids.RoiderTags
import roiderUnion.nomads.NomadJPWarningPlugin
import roiderUnion.nomads.NomadsHelper

class RoiderCampaignPlugin : BaseCampaignPlugin() {
    companion object {
        const val IS_ASTEROID_KEY = "\$isAsteroid"
    }


    override fun updateEntityFacts(entity: SectorEntityToken?, memory: MemoryAPI?) {
        if (entity is AsteroidAPI) {
//            Memory.setFlag(IS_ASTEROID_KEY, ModIds.ROIDER_UNION, entity, 0f)
        }
    }

    override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken?): PluginPick<InteractionDialogPlugin>? {
        if (interactionTarget is JumpPointAPI && NomadsHelper.isNomadSystem(interactionTarget, true)) {
            return PluginPick(NomadJPWarningPlugin(), PickPriority.MOD_SET)
        }
//        if (interactionTarget is AsteroidAPI) {
//            return PluginPick(RuleBasedInteractionDialogPluginImpl(), PickPriority.MOD_GENERAL)
//        }
        return super.pickInteractionDialogPlugin(interactionTarget)
    }
}