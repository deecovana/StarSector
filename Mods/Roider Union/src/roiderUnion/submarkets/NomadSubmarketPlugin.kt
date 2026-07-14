package roiderUnion.submarkets

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.campaign.PlayerMarketTransaction
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.SubmarketPlugin.PlayerEconomyImpactMode
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.impl.campaign.submarkets.BlackMarketPlugin
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderIndustries
import roiderUnion.ids.RoiderItems
import java.util.*


class NomadSubmarketPlugin : BlackMarketPlugin() {
    companion object {
        const val QUANTITY_REDUCTION = 0.2f
        const val FUEL_MULT = 2f

        fun alias(x: XStream) {
            x.alias(Aliases.NOMMARK, NomadSubmarketPlugin::class.java)
        }
    }

    override fun isBlackMarket(): Boolean = false

    override fun getTariff(): Float = market.tariff.modifiedValue

    override fun getPlayerEconomyImpactMode(): PlayerEconomyImpactMode = PlayerEconomyImpactMode.PLAYER_SELL_ONLY

    override fun getStockpileLimit(com: CommodityOnMarketAPI?): Int {
        if (com == null) return 0
        var limit = OpenMarketPlugin.getBaseStockpileLimit(com)

        val random = Random(
            (market.id.hashCode() + submarket.specId.hashCode() + (Helper.sector?.clock?.month ?: 0) * 170000).toLong()
        )
        limit *= 0.9f + 0.2f * random.nextFloat()
        val sm = market.stabilityValue / 10f
        limit *= QUANTITY_REDUCTION * sm
        if (limit < 0) limit = 0f
        if (com.isFuel) limit *= FUEL_MULT
        return limit.toInt()
    }

    override fun updateCargoPrePlayerInteraction() {
        super.updateCargoPrePlayerInteraction()

        cargo?.removeItems(
            CargoAPI.CargoItemType.SPECIAL,
            SpecialItemData(Items.INDUSTRY_BP, RoiderIndustries.DIVES),
            1f
        )
        cargo?.removeItems(
            CargoAPI.CargoItemType.SPECIAL,
            SpecialItemData(RoiderItems.DIVES_BP, RoiderIndustries.DIVES),
            1f
        )
        if (Helper.sector?.playerFaction?.knowsIndustry(RoiderIndustries.DIVES) == false) {
            cargo?.addSpecial(SpecialItemData(RoiderItems.DIVES_BP, RoiderIndustries.DIVES), 1f)
        }
    }

    override fun reportPlayerMarketTransaction(transaction: PlayerMarketTransaction?) {
        if (transaction == null) return
        if (!isParticipatesInEconomy) return

        val mode = playerEconomyImpactMode
        SharedData.getData().playerActivityTracker.getPlayerTradeData(submarket).addTransaction(transaction)


        for (stack in transaction.sold.stacksCopy) {
            if (stack.isCommodityStack) {
                val qty = stack.size * playerTradeImpactMult
                if (qty <= 0) continue
                val com = market.getCommodityData(stack.commodityId)
                when (mode) {
                    PlayerEconomyImpactMode.BOTH -> {
                        com.addTradeMod("sell_" + Misc.genUID(), qty, TRADE_IMPACT_DAYS)
                    }
                    PlayerEconomyImpactMode.PLAYER_SELL_ONLY -> {
                        com.addTradeModPlus("sell_" + Misc.genUID(), qty, TRADE_IMPACT_DAYS)
                    }
                    PlayerEconomyImpactMode.PLAYER_BUY_ONLY, PlayerEconomyImpactMode.NONE -> {
                        com.addTradeModMinus("sell_" + Misc.genUID(), qty, TRADE_IMPACT_DAYS)
                    }
                }
            }
        }
        for (stack in transaction.bought.stacksCopy) {
            if (stack.isCommodityStack) {
                val qty = stack.size * playerTradeImpactMult
                if (qty <= 0) continue
                val com = market.getCommodityData(stack.commodityId)
                if (mode == PlayerEconomyImpactMode.BOTH) {
                    com.addTradeMod("buy_" + Misc.genUID(), -qty, TRADE_IMPACT_DAYS)
                } else if (mode == PlayerEconomyImpactMode.PLAYER_SELL_ONLY || mode == PlayerEconomyImpactMode.NONE) {
                    com.addTradeModPlus("buy_" + Misc.genUID(), -qty, TRADE_IMPACT_DAYS)
                } else if (mode == PlayerEconomyImpactMode.PLAYER_BUY_ONLY) {
                    com.addTradeModMinus("buy_" + Misc.genUID(), -qty, TRADE_IMPACT_DAYS)
                }
            }
        }
    }

    override fun getTooltipAppendix(ui: CoreUIAPI?): String? = null
}