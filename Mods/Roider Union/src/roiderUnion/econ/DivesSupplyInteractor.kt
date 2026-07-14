package roiderUnion.econ

import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderIds
import roiderUnion.ids.RoiderIndustries

class DivesSupplyInteractor(
    private val model: DivesModel,
    @Transient var supply: (String, Int) -> Unit,
    @Transient var supply2: (String, String, Int, String) -> Unit,
    @Transient var demand: (String, Int) -> Unit,
    @Transient var getRemoteMiningDesc: (RemoteRezSource) -> String,
    @Transient var getRemoteMiningBonusDesc: (String, Boolean) -> String,
    @Transient var isUnionHQ: () -> Boolean,
    private val miningRange: MutableStat
) {
    companion object {
        fun alias(x: XStream) {
            val jClass = DivesSupplyInteractor::class.java
            x.alias(Aliases.DIVESSI, jClass)
            x.aliasAttribute(jClass, "market", "m")
            x.aliasAttribute(jClass, "model", "mo")
        }

        @JvmField
        val BASE_SUPPLY = mutableMapOf(
            Pair(Commodities.ORE, 1),
            Pair(Commodities.RARE_ORE, -1),
            Pair(Commodities.ORGANICS, 1),
            Pair(Commodities.VOLATILES, -1)
        )
        @JvmField
        val BONUS_SUPPLY = mutableMapOf(
            Pair(Commodities.ORE + Conditions.ORE_ULTRARICH, 3),
            Pair(Commodities.ORE + Conditions.ORE_RICH, 2),
            Pair(Commodities.ORE + Conditions.ORE_ABUNDANT, 1),
            Pair(Commodities.ORE + Conditions.ORE_MODERATE, 0),
            Pair(Commodities.ORE + Conditions.ORE_SPARSE, -1),
            Pair(Commodities.RARE_ORE + Conditions.RARE_ORE_ULTRARICH, 3),
            Pair(Commodities.RARE_ORE + Conditions.RARE_ORE_RICH, 2),
            Pair(Commodities.RARE_ORE + Conditions.RARE_ORE_ABUNDANT, 1),
            Pair(Commodities.RARE_ORE + Conditions.RARE_ORE_MODERATE, 0),
            Pair(Commodities.RARE_ORE + Conditions.RARE_ORE_SPARSE, -1),
            Pair(Commodities.ORGANICS + Conditions.ORGANICS_PLENTIFUL, 2),
            Pair(Commodities.ORGANICS + Conditions.ORGANICS_ABUNDANT, 1),
            Pair(Commodities.ORGANICS + Conditions.ORGANICS_COMMON, 0),
            Pair(Commodities.ORGANICS + Conditions.ORGANICS_TRACE, -1),
            Pair(Commodities.VOLATILES + Conditions.VOLATILES_PLENTIFUL, 2),
            Pair(Commodities.VOLATILES + Conditions.VOLATILES_ABUNDANT, 1),
            Pair(Commodities.VOLATILES + Conditions.VOLATILES_DIFFUSE, 0),
            Pair(Commodities.VOLATILES + Conditions.VOLATILES_TRACE, -1)
        )
        @JvmField
        val MAX_BONUS = mutableMapOf(
            Pair(Commodities.ORE, 3),
            Pair(Commodities.RARE_ORE, 3),
            Pair(Commodities.ORGANICS, 2),
            Pair(Commodities.VOLATILES, 2),
        )
    }

    @Transient private lateinit var remoteSource: MutableMap<String, RemoteRezSource>
    @Transient private lateinit var remoteSupply: MutableMap<String, String>

    private lateinit var market: MarketAPI
    private var miningRangeCheck = IntervalUtil(
        DivesInteractor.MINING_RANGE_CHECK_DAYS_MIN,
        DivesInteractor.MINING_RANGE_CHECK_DAYS_MAX
    )
    private var systemsInRange = mutableListOf<StarSystemAPI>()
    init {
        updateSystemsInRange()
    }

    fun init(market: MarketAPI?) {
        this.market = market ?: throw NullPointerException("market should never be null - please report this")
        updateSystemsInRange()
    }

    fun advance(amount: Float) {
        miningRangeCheck.advance(Misc.getDays(amount))
        if (!miningRangeCheck.intervalElapsed()) return
        updateSystemsInRange()
    }

    fun updateSystemsInRange() {
        if (!this::market.isInitialized) return
        systemsInRange.clear()
        val location = market.locationInHyperspace
        for (s in Helper.sector?.starSystems ?: listOf()) {
            if (Misc.getDistanceLY(s.location, location) <= miningRange.modifiedValue) {
                systemsInRange += s
            }
        }
    }

    fun applySupplyAndDemand(
        getMaxDeficit: (Array<String>) -> com.fs.starfarer.api.util.Pair<String, Int>,
        applyDeficitToProduction: (Int, com.fs.starfarer.api.util.Pair<String, Int>, Array<String>) -> Unit
    ) {
        val extra = if (isUnionHQ()) 1 else 0
        val size = if (isUnionHQ()) market.size else DivesInteractor.BASE_SIZE
        val noDrugs = market.hasCondition(RoiderIds.Roider_Conditions.PARASITE_SPORES)
                || market.hasCondition(RoiderIds.Roider_Conditions.PSYCHOACTIVE_FUNGUS)
        val drugs = if (noDrugs) 0 else DivesInteractor.BASE_SIZE
        demand(Commodities.HEAVY_MACHINERY, size - 2)
        demand(Commodities.DRUGS, drugs)
        demand(Commodities.SUPPLIES, size - 1 + extra)
        demand(Commodities.FUEL, size - 3 + extra)
//        demand(Commodities.SHIPS, size - 1 + extra)
        supply(Commodities.CREW, size)
        val deficit: com.fs.starfarer.api.util.Pair<String, Int> = getMaxDeficit(arrayOf(Commodities.HEAVY_MACHINERY))
        applyDeficitToProduction(
            0, deficit,
            arrayOf(Commodities.ORE, Commodities.RARE_ORE, Commodities.ORGANICS, Commodities.VOLATILES)
        )
    }

     fun calculateRemoteResources() {
         remoteSource = mutableMapOf()
         remoteSupply = mutableMapOf()
         calculateRemoteSupply(Commodities.ORE)
         calculateRemoteSupply(Commodities.RARE_ORE)
         calculateRemoteSupply(Commodities.ORGANICS)
         calculateRemoteSupply(Commodities.VOLATILES)
    }

    private fun calculateRemoteSupply(com: String) {
        val oreBonus = getMiningBonus(com)
        val isVoidOre = oreBonus != getMarketBonus(com, market.conditions)
        if (oreBonus == null) {
            remoteSupply.remove(com)
            remoteSource.remove(com)
        } else {
            remoteSupply[com] = oreBonus
            remoteSource[com] = if (isVoidOre) RemoteRezSource.VOID else RemoteRezSource.BASE
        }
    }

    private fun getMiningBonus(com: String): String? {
        val marketCond = getMarketBonus(com, market.conditions)
        var maxBonus = BONUS_SUPPLY[com + marketCond] ?: -1
        var cond = marketCond
        for (s in systemsInRange) {
            val c = DivesSupplyManager.getMiningBonus(s, com) ?: continue
            val bonus  = BONUS_SUPPLY[com + c] ?: -1
            if (bonus > maxBonus) {
                maxBonus = bonus
                cond = c
            }
            if (bonus > (MAX_BONUS[com] ?: 100)) break
        }
        return cond
    }

    private fun getMarketBonus(com: String, conditions: List<MarketConditionAPI>): String? {
        return conditions.firstOrNull { BONUS_SUPPLY[com + it.id] != null }?.id
    }

    fun supplyRemoteResources(
        sporesDesc: String
    ) {
        val hasParasites = market.hasCondition(RoiderIds.Roider_Conditions.PARASITE_SPORES)
        supplyRemote(Commodities.ORE, hasParasites, sporesDesc)
        supplyRemote(Commodities.RARE_ORE, hasParasites, sporesDesc)
        supplyRemote(Commodities.VOLATILES, hasParasites, sporesDesc)
        supplyRemote(Commodities.ORGANICS, hasParasites, sporesDesc)
    }

    private fun supplyRemote(
        com: String,
        hasParasites: Boolean,
        sporesDesc: String
    ) {
        if (!this::remoteSupply.isInitialized) calculateRemoteResources()
        if (remoteSupply.containsKey(com)) {
            supplyBase(com)
            supplyBonus(com)
            if (hasParasites) supplyParasites(com, sporesDesc)
        }
    }

    private fun supplyBase(com: String) {
        supply2(
            RoiderIndustries.DIVES + "_" + com,
            com,
            BASE_SUPPLY[com] ?: 0,
            getRemoteMiningDesc(remoteSource[com] ?: RemoteRezSource.BASE)
        )
    }

    private fun supplyBonus(com: String) {
        supply2(
            RoiderIndustries.DIVES + "_" + com + "_mod",
            com,
            BONUS_SUPPLY[com + remoteSupply[com]] ?: 0,
            getRemoteMiningBonusDesc(remoteSupply[com] ?: "", remoteSource[com] == RemoteRezSource.VOID)
        )
    }

    private fun supplyParasites(com: String, parasitesDesc: String) {
        supply2(
            RoiderIndustries.DIVES + "_" + com + "_" + RoiderIds.Roider_Conditions.PARASITE_SPORES,
            com,
            DivesInteractor.PARASITES_PENALTY,
            parasitesDesc
        )
    }

    fun selectPostDescription() {
        model.canMine = canMine()
        model.resources = getResources()
    }

    private fun getResources(): String {
        if (!this::remoteSupply.isInitialized) calculateRemoteResources()
        val resources = mutableListOf<String>()
        remoteSupply.keys
            .filter { (BASE_SUPPLY[it]?.plus(BONUS_SUPPLY[it + remoteSupply[it]!!]!!) ?: 0) > 0 }
            .forEach { resources.add(getCommodityName(it)) }
        return Misc.getAndJoined(resources)
    }

    private fun getCommodityName(com: String): String {
        return Helper.settings?.getCommoditySpec(com)?.name?.lowercase() ?: ExternalStrings.DEBUG_NULL
    }

    fun canMine(): Boolean {
        if (systemsInRange.isEmpty()) updateSystemsInRange()
        if (!this::remoteSupply.isInitialized) calculateRemoteResources()
        return remoteSupply.isNotEmpty()
    }
}