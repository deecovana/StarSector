package roiderUnion.econ

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Items
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIndustries
import roiderUnion.ids.RoiderItems

class DivesBPOpenMarketAdder : EconomyUpdateListener {

    override fun isEconomyListenerExpired(): Boolean = false

    override fun commodityUpdated(commodityId: String?) {}

    override fun economyUpdated() {
        Helper.sector?.economy?.marketsCopy?.filter { Helper.hasRoiders(it) }?.map { it.submarketsCopy }?.forEach {
            list -> list.filter { it.plugin?.isOpenMarket == true }.forEach { reportSubmarketCargoAndShipsUpdated(it) }
        }
    }

    private fun reportSubmarketCargoAndShipsUpdated(submarket: SubmarketAPI?) {
        if (submarket == null) return
        if (submarket.plugin?.isOpenMarket == true
            && Helper.hasRoiders(submarket.market)
            && submarket.plugin?.okToUpdateShipsAndWeapons() == true
        ) {
            val cargo = submarket.cargo ?: return
            cargo.removeItems(
                CargoAPI.CargoItemType.SPECIAL,
                SpecialItemData(Items.INDUSTRY_BP, RoiderIndustries.DIVES),
                1f
            )
            cargo.removeItems(
                CargoAPI.CargoItemType.SPECIAL,
                SpecialItemData(RoiderItems.DIVES_BP, RoiderIndustries.DIVES),
                1f
            )
            if (Helper.sector?.playerFaction?.knowsIndustry(RoiderIndustries.DIVES) == false) {
                cargo.addSpecial(SpecialItemData(RoiderItems.DIVES_BP, RoiderIndustries.DIVES), 1f)
            }
        }
    }
}