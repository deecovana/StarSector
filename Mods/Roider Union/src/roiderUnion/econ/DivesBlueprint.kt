package roiderUnion.econ

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.IndustryBlueprintItemPlugin
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIndustries

/**
 * Author: SafariJohn
 */
class DivesBlueprint : IndustryBlueprintItemPlugin() {
    companion object {
        const val BASE_VALUE = 50000

        fun aliasAttributes(x: XStream?) {}
    }

    override fun init(stack: CargoStackAPI?) {
        if (stack == null) return
        this.stack = stack
        stack.specialDataIfSpecial?.data = RoiderIndustries.DIVES
        industry = Helper.settings?.getIndustrySpec(RoiderIndustries.DIVES)
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        return if (industry != null) BASE_VALUE
        else super.getPrice(market, submarket)
    }
}