package roiderUnion.econ

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.IndustryBlueprintItemPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderIndustries
import java.awt.Color

/**
 * Author: SafariJohn
 */
class UnionHQBlueprint : IndustryBlueprintItemPlugin() {
    companion object {
        const val BASE_VALUE = 200000

        fun alias(x: XStream) {
            x.alias(Aliases.HQBP, UnionHQBlueprint::class.java)
        }
    }

    override fun init(stack: CargoStackAPI?) {
        if (stack == null) return
        this.stack = stack
        stack.specialDataIfSpecial?.data = RoiderIndustries.UNION_HQ
        industry = Helper.settings?.getIndustrySpec(RoiderIndustries.UNION_HQ)
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        return if (industry != null) BASE_VALUE
        else super.getPrice(market, submarket)
    }

    override fun createTooltip(
        tooltip: TooltipMakerAPI?,
        expanded: Boolean,
        transferHandler: CargoTransferHandlerAPI?,
        stackSource: Any?
    ) {
        if (Helper.anyNull(tooltip)) return
        createTooltip(tooltip, expanded, transferHandler, stackSource, true)
        val industryId: String = stack.specialDataIfSpecial?.data ?: ""
        val known = Helper.sector?.playerFaction?.knowsIndustry(industryId) == true
        tooltip!!.addPara(industry.desc, Helper.PAD)
        val desc = ExternalStrings.BP_HQ_DIVES
        val c = Helper.roiders?.baseUIColor ?: Color.RED
        tooltip.addPara(desc, c, Helper.PAD)
        addCostLabel(tooltip, Helper.PAD, transferHandler, stackSource)
        if (known) {
            tooltip.addPara(ExternalStrings.BP_KNOWN, Misc.getGrayColor(), Helper.PAD)
        } else {
            tooltip.addPara(ExternalStrings.BP_LEARN, Misc.getPositiveHighlightColor(), Helper.PAD)
        }
    }
}