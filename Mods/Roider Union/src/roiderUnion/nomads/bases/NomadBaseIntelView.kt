package roiderUnion.nomads.bases

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.Categories
import roiderUnion.ids.Icons

class NomadBaseIntelView(private val model: NomadBaseIntelModel) {
    companion object {
        fun alias(x: XStream) {
            val jClass = NomadBaseIntelView::class.java
            x.alias(Aliases.NBIV, jClass)
            x.aliasAttribute(jClass, "model", "m")
        }

        const val TOKEN_FACTION = "\$faction"
        const val TOKEN_NAME = "\$name"
        const val TOKEN_A_AN = "\$aAn"
        const val TOKEN_SYSTEM = "\$system"
        const val LOGO_HEIGHT = 128f
    }

    fun getName(): String {
        val result = when (model.nomadBaseState) {
            NomadBaseState.KNOWN -> ExternalStrings.NOMAD_BASE_KNOWN
            NomadBaseState.DISCOVERED -> ExternalStrings.NOMAD_BASE_DISCOVERED
            NomadBaseState.LOC_UNKNOWN -> ExternalStrings.NOMAD_BASE_LOC_UNKNOWN
            NomadBaseState.DESTROYED -> ExternalStrings.NOMAD_BASE_DESTROYED
            NomadBaseState.ABANDONED -> ExternalStrings.NOMAD_BASE_ABANDONED
            null -> ExternalStrings.DEBUG_NULL
        }
        val prefix = model.base.faction.personNamePrefix
        return result.replace(TOKEN_FACTION, prefix).replace(TOKEN_NAME, model.base.name)
    }

    fun getFactionForUIColors(): FactionAPI = model.base.faction

    fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val faction: FactionAPI = model.base.faction
        info.addImage(faction.logo, width, LOGO_HEIGHT, Helper.PAD) // extern logo height?

        val factionPrefix = faction.personNamePrefix
        val aAn = faction.personNamePrefixAOrAn
        val baseDesc = ExternalStrings.NOMAD_BASE_BASE_DESC
            .replace(TOKEN_A_AN, aAn)
            .replace(TOKEN_FACTION, factionPrefix)
            .replace(TOKEN_SYSTEM, model.base.containingLocation.nameWithLowercaseType)
        info.addPara(baseDesc, Helper.PAD, faction.baseUIColor, factionPrefix)

        val desc = when (model.nomadBaseTier) {
            NomadBaseLevel.STARTING -> ExternalStrings.NOMAD_BASE_STARTING_DESC
            NomadBaseLevel.ESTABLISHED -> ExternalStrings.NOMAD_BASE_ESTABLISHED_DESC
            NomadBaseLevel.BATTLESTATION -> ExternalStrings.NOMAD_BASE_BATTLESTATION_DESC
            NomadBaseLevel.SHIPWORKS -> ExternalStrings.NOMAD_BASE_SHIPWORKS_DESC
            NomadBaseLevel.HQ -> ExternalStrings.NOMAD_BASE_HQ_DESC
            NomadBaseLevel.CAPITAL -> ExternalStrings.NOMAD_BASE_CAPITAL_DESC
            null -> ExternalStrings.NOMAD_BASE_UNKNOWN_DESC
        }
        if (model.nomadBaseTier == NomadBaseLevel.CAPITAL) {
            val modDesc = desc.replace(TOKEN_FACTION, faction.displayNameWithArticle)
            info.addPara(modDesc, Helper.PAD, faction.baseUIColor, faction.displayName)
        } else {
            info.addPara(desc, Helper.PAD)
        }

        info.addSectionHeading(
            ExternalStrings.NOMAD_BASE_RECENT_EVENTS,
            faction.baseUIColor,
            faction.darkUIColor,
            Alignment.MID,
            Helper.PAD
        )

        if (model.nomadBaseState == NomadBaseState.DESTROYED) {
            info.addPara(ExternalStrings.NOMAD_BASE_DESTROYED_EVENT, Helper.PAD)
        } else if (model.nomadBaseState == NomadBaseState.ABANDONED) {
            info.addPara(ExternalStrings.NOMAD_BASE_ABANDONED_EVENT, Helper.PAD)
        }
    }

    fun getIcon(): String? {
        return Helper.settings?.getSpriteName(Categories.INTEL, Icons.NOMAD_BASE)
    }
}
