package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.getSourceMarket
import org.magiclib.kotlin.isPatrol
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.nomads.NomadsHelper

class Roider_NomadWarning : BaseCommandPlugin() {
    companion object {
        const val COMMAND_GIVEN = "given"
        const val TOKEN_FACTION = "\$faction"
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        val entity = dialog?.interactionTarget ?: return false
        if (params?.isEmpty() == true) {
            return if (entity is CampaignFleetAPI) isPatrolWarning(entity)
            else return isSystemWarning(entity)
        } else if (params?.get(0)?.getString(memoryMap) == COMMAND_GIVEN && entity is CampaignFleetAPI) {
            discoverBase(entity, dialog.textPanel)
        }

        return false
    }

    private fun isSystemWarning(entity: SectorEntityToken): Boolean {
        val jp = entity as? JumpPointAPI ?: return false
        val nomadBase = NomadsHelper.bases
            .firstOrNull { base -> jp.destinations.any { base.starSystem === it.destination.starSystem } } ?: return false
        return !nomadBase.isDiscoverable
    }

    private fun discoverBase(fleet: CampaignFleetAPI, text: TextPanelAPI?) {
        val source = NomadsHelper.bases.firstOrNull { it.market === fleet.getSourceMarket() } ?: return
        val plugin = Helper.sector?.genericPlugins?.getPluginsOfClass(DiscoverEntityPlugin::class.java)?.get(0) as? DiscoverEntityPlugin ?: return
        plugin.discoverEntity(source)
        text?.addPara(
            ExternalStrings.NOMAD_BASE_DISCOVERED.replace(TOKEN_FACTION, source.faction.personNamePrefix),
            Misc.getPositiveHighlightColor(),
            source.faction.color,
            source.faction.personNamePrefix
        )
    }

    private fun isPatrolWarning(fleet: CampaignFleetAPI): Boolean {
        val source = NomadsHelper.bases.firstOrNull { it.market === fleet.getSourceMarket() } ?: return false
        return source.isDiscoverable && fleet.isPatrol()
    }
}