package roiderUnion.econ

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.IndustryBlueprintItemPlugin
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderIndustries

/**
 * Author: SafariJohn
 */
class ShipworksBlueprint : IndustryBlueprintItemPlugin() {
    companion object {
        const val BASE_VALUE = 140000

        fun alias(x: XStream) {
            x.alias(Aliases.SWBP, ShipworksBlueprint::class.java)
        }
    }

    override fun init(stack: CargoStackAPI) {
        this.stack = stack
        stack.specialDataIfSpecial?.data = RoiderIndustries.SHIPWORKS
        industry = Helper.settings?.getIndustrySpec(RoiderIndustries.SHIPWORKS)
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        return if (industry != null) BASE_VALUE
        else super.getPrice(market, submarket)
    }
}