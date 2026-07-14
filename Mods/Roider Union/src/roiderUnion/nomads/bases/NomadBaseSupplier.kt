package roiderUnion.nomads.bases

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.CommoditySourceType
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.MutableStatWithTempMods
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.MemoryKeys
import roiderUnion.nomads.NomadsHelper
import kotlin.math.roundToInt

class NomadBaseSupplier : EconomyUpdateListener {
    companion object {
        const val MOD_ID = MemoryKeys.NOMAD_BASE + "_supply"
    }

    @Transient
    private lateinit var commodities: MutableMap<String, MutableSet<SectorEntityToken>>
    @Transient
    private lateinit var commoditiesByBase: MutableMap<String, MutableSet<String>>

    override fun commodityUpdated(commodityId: String?) {
        if (commodityId == null) return
        if (!this::commodities.isInitialized) commodities = mutableMapOf()
        if (!this::commoditiesByBase.isInitialized) commoditiesByBase = mutableMapOf()
        val coms = mutableSetOf<SectorEntityToken>()
        for (base in NomadsHelper.bases) {
            val comsByBase = commoditiesByBase[base.id] ?: mutableSetOf()
            val com = base.market?.getCommodityData(commodityId) ?: continue
            val supply = com.availableStat ?: continue
            if (com.maxDemand > getLocalSupply(supply)) {
                coms += base
                comsByBase += commodityId
            }
            commoditiesByBase[base.id] = comsByBase
        }
        commodities[commodityId] = coms
    }

    override fun economyUpdated() {
        if (!this::commodities.isInitialized) commodities = mutableMapOf()
        if (!this::commoditiesByBase.isInitialized) commoditiesByBase = mutableMapOf()
        val commoditiesByBaseL = commoditiesByBase
        cleanMissingBases(commoditiesByBaseL)
        val coreMarkets = Helper.sector?.economy?.getMarketsInGroup(null) ?: emptyList()
        val inFaction = mutableMapOf<String, Int>()
        val outFaction = mutableMapOf<String, Int>()
        val commoditiesL = commodities
        for (market in coreMarkets) {
            for (cid in commoditiesL.keys) {
                val com = market.getCommodityData(cid) ?: continue
                for (base in commoditiesL[cid] ?: emptySet()) {
                    if (market.factionId == base.faction.id) inFaction[base.id + cid] = com.maxSupply.coerceAtLeast((inFaction[base.id + cid] ?: 0))
                    else outFaction[base.id + cid] = com.maxSupply.coerceAtLeast((outFaction[base.id + cid] ?: 0))
                }
            }
        }
        for (base in NomadsHelper.bases) {
            val coms = commoditiesByBaseL[base.id] ?: continue
            for (cid in coms) {
                val com = base.market?.getCommodityData(cid) ?: continue
                val inFactionSupply = inFaction[base.id + cid] ?: 0
                val outFactionSupply = outFaction[base.id + cid] ?: 0
                if (inFactionSupply >= com.maxDemand) supplyInFaction(base.market, com, inFactionSupply)
                else supplyOutFaction(base.market, com, outFactionSupply)
            }
            base.market?.reapplyIndustries()
        }
    }

    private fun cleanMissingBases(commoditiesByBaseL: MutableMap<String, MutableSet<String>>) {
        for (baseId in commoditiesByBaseL.keys.toList()) {
            if (NomadsHelper.bases.none { it.id == baseId }) commoditiesByBaseL.remove(baseId)
        }
    }

    private fun supplyInFaction(market: MarketAPI?, com: CommodityOnMarketAPI, inFactionSupply: Int) {
        if (market == null) return
        val supply = com.availableStat ?: return
        val supplyBonus = inFactionSupply.coerceAtMost(com.maxDemand) - getLocalSupply(supply)
        com.commodityMarketData.getMarketShareData(market).source = CommoditySourceType.IN_FACTION
        com.maxSupply = inFactionSupply.coerceAtMost(com.maxDemand) // Not ideal bc it makes it "produced", but close enough to functional
        supply.modifyFlat(MOD_ID, supplyBonus.toFloat(), ExternalStrings.NOMAD_SUPPLY_IN_FACTION)
    }

    private fun supplyOutFaction(market: MarketAPI?, com: CommodityOnMarketAPI, outFactionSupply: Int) {
        if (market == null) return
        val supply = com.availableStat ?: return
        val supplyBonus = outFactionSupply.coerceAtMost(com.maxDemand) - getLocalSupply(supply)
        com.commodityMarketData.getMarketShareData(market).source = CommoditySourceType.GLOBAL
        supply.modifyFlat(MOD_ID, supplyBonus.toFloat(), ExternalStrings.NOMAD_SUPPLY)
    }

    private fun getLocalSupply(supply: MutableStatWithTempMods): Int {
        var result = supply.baseValue.roundToInt()
        result += supply.flatMods?.values
            ?.filterNot { it.source == MOD_ID }
            ?.sumOf { it.value.roundToInt() }
            ?: 0
        return result
    }

    override fun isEconomyListenerExpired(): Boolean = false
}