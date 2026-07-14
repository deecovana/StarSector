package roiderUnion.econ

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.combat.MutableStat.StatMod
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderIds
import java.awt.Color

class DivesView(private val model: DivesModel) {
    companion object {
        const val TOKEN_MARKET = "\$market"
        const val TOKEN_FACTION = "\$theMarketFaction"
        const val TOKEN_RESOURCES = "\$resources"
        const val TOKEN_MILITARY = "\$supplement"
        const val TOKEN_CONDITION = "\$condition"

        fun alias(x: XStream) {
            val jClass = DivesView::class.java
            x.alias(Aliases.DIVESV, jClass)
            x.aliasAttribute(jClass, "model", "m")
        }
    }

    val image: String
        get() = model.image

    fun showPostDescription(tooltip: TooltipMakerAPI) {
        if (model.isUnionHQ) {
            showUnionDescription(tooltip)
        } else {
            if (model.canMine) {
                val text = ExternalStrings.DIVES_MINING.replace(TOKEN_RESOURCES, model.resources)
                tooltip.addPara(text, Helper.PAD)
            } else {
                val text = ExternalStrings.DIVES_NO_MINING.replace(TOKEN_MARKET, model.marketName)
                tooltip.addPara(text, Misc.getNegativeHighlightColor(), Helper.PAD)
            }
        }
    }

    private fun showUnionDescription(tooltip: TooltipMakerAPI) {
        if (!model.isUnionHQ) return

        if (model.isHostile) {
            val text = ExternalStrings.HQ_DISABLED
                .replace(TOKEN_FACTION, model.theMarketFaction)
                .replace(TOKEN_MARKET, model.marketName)
            tooltip.addPara(text, Misc.getNegativeHighlightColor(), Helper.PAD)
            return
        }

        if (!model.showPostDescription) return

        if (model.canMine) {
            val text = ExternalStrings.HQ_MINING
                .replace(TOKEN_MARKET, model.marketName)
                .replace(TOKEN_RESOURCES, model.resources)
            tooltip.addPara(text, Helper.PAD)
        } else {
            val text = ExternalStrings.HQ_NO_MINING.replace(TOKEN_MARKET, model.marketName)
            tooltip.addPara(text, Misc.getNegativeHighlightColor(), Helper.PAD)
        }

        val milText = if (model.isMilitary) ExternalStrings.HQ_MILITARY_FULL else ExternalStrings.HQ_MILITARY_PARTIAL
        val text = ExternalStrings.HQ_EXTRA
            .replace(TOKEN_MARKET, model.marketName)
            .replace(TOKEN_MILITARY, milText)
        val conversionText = if (model.isFullConversions) "" else ExternalStrings.HQ_LIMITED_CONVERSIONS
        tooltip.addPara(text + conversionText, Helper.PAD)
    }

    fun showPostDemandSection(
        tooltip: TooltipMakerAPI,
        hasDemand: Boolean,
        mode: Industry.IndustryTooltipMode,
        addStabilityPostDemandSection: (TooltipMakerAPI?, Boolean, Industry.IndustryTooltipMode?) -> Unit
    ) {
        if (model.isDrugsDeficit) {
            val text = ExternalStrings.DIVES_DRUGS
                .replace(TOKEN_CONDITION, BaseIndustry.getDeficitText(Commodities.DRUGS))
            tooltip.addPara(text, Helper.SMALL_PAD, Misc.getHighlightColor(), model.drugsDeficit)
        }
        if (model.isUnionHQ) {
            addStabilityPostDemandSection(tooltip, hasDemand, mode)
            addUnionDefenseImpactSection(tooltip)
        }
        if (model.militiaEnabled) addMilitiaImpactSection(tooltip)
    }

    private fun addUnionDefenseImpactSection(tooltip: TooltipMakerAPI) {
        val stat = MutableStat(1f).apply { modifyFlat("1", model.defenseBonus - 1f, getNameForModifier()) }
        val total = Misc.getRoundedValueFloat(stat.modifiedValue)
        val totalStr = Strings.X + total
        tooltip.addPara(ExternalStrings.GROUND_DEFENSE, Helper.PAD, Misc.getHighlightColor(), totalStr)
        tooltip.addStatModGrid(400f, 35f, Helper.PAD, Helper.SMALL_PAD, stat, object : StatModValueGetter {
            override fun getPercentValue(mod: StatMod): String? = null
            override fun getMultValue(mod: StatMod): String? = null
            override fun getModColor(mod: StatMod): Color? = null
            override fun getFlatValue(mod: StatMod): String {
                val r = Misc.getRoundedValue(mod.value)
                return if (mod.value >= 0) "+$r" else r
            }
        })
    }

    private fun addMilitiaImpactSection(tooltip: TooltipMakerAPI) {
        val stat = MutableStat(0f).apply { modifyFlat("1", model.militiaStrength, ExternalStrings.DIVES_MILITIA_DEFENSE) }
        val total = Misc.getRoundedValueFloat(stat.modifiedValue)
        val totalStr = "+$total"
        tooltip.addPara(ExternalStrings.GROUND_DEFENSE, Helper.PAD, Misc.getHighlightColor(), totalStr)
        tooltip.addStatModGrid(400f, 35f, Helper.PAD, Helper.SMALL_PAD, stat, object : StatModValueGetter {
            override fun getPercentValue(mod: StatMod): String? = null
            override fun getMultValue(mod: StatMod): String? = null
            override fun getModColor(mod: StatMod): Color? = null
            override fun getFlatValue(mod: StatMod): String {
                val r = Misc.getRoundedValue(mod.value)
                return if (mod.value >= 0) "+$r" else r
            }
        })
    }

    fun getRemoteMiningDesc(source: RemoteRezSource): String {
        return when (source) {
            RemoteRezSource.BASE -> ExternalStrings.BASE_VALUE
            RemoteRezSource.VOID -> ExternalStrings.BASE_VALUE + ExternalStrings.DIVES_VOID
            RemoteRezSource.PLANET -> ExternalStrings.BASE_VALUE + ExternalStrings.DIVES_PLANET
            RemoteRezSource.SYSTEM -> ExternalStrings.BASE_VALUE + ExternalStrings.DIVES_SYSTEM
        }
    }

    private fun getConditionName(cond: String): String {
        return Misc.ucFirst(Helper.settings?.getMarketConditionSpec(cond)?.name?.lowercase()) ?: ExternalStrings.DEBUG_NULL
    }

    private fun replaceDepositsWithRoids(desc: String): String {
        return desc.replace(ExternalStrings.DIVES_DR_FIND, ExternalStrings.DIVES_DR_REPLACE)
    }

    fun getRemoteMiningBonusDesc(cond: String, isRoids: Boolean): String {
        return if (isRoids) replaceDepositsWithRoids(getConditionName(cond))
        else getConditionName(cond)
    }

    fun getNameForModifier(): String {
        return if (model.isUnionHQ) model.nameForModifier else Misc.ucFirst(model.nameForModifier)
    }

    fun getUnavailableReason(): String {
        return if (model.isUnionHQ) {
            if (!model.isHqBpKnown) ExternalStrings.HQ_BP_REQUIRED
            else model.defaultUnavailableReason
        } else if (!model.isDivesBpKnown) {
            ExternalStrings.DIVES_BP_REQUIRED
        } else {
            ExternalStrings.INDUSTRY_REQ_SPACEPORT
        }
    }

    fun getDrugsShortageDesc(): String = ExternalStrings.DIVES_DRUGS_SHORTAGE

    fun getSporesDesc(): String {
        val sporesSpec = Helper.settings?.getMarketConditionSpec(RoiderIds.Roider_Conditions.PARASITE_SPORES)
        return Misc.ucFirst(sporesSpec?.name ?: ExternalStrings.PARASITIC_SPORES)
    }
}