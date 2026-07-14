package roiderUnion.econ

import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.*
import com.fs.starfarer.api.characters.ImportantPeopleAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition
import com.fs.starfarer.api.loading.IndustrySpecAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Pair
import com.thoughtworks.xstream.XStream
import roiderUnion.fleets.UnionHQPatrolManager
import roiderUnion.helpers.*
import roiderUnion.ids.*
import java.util.*
import kotlin.reflect.KProperty0

class DivesInteractor(
    private val model: DivesModel,
    @Transient var isUnionHQ: KProperty0<Boolean>,
    private val miningRange: MutableStat
) {
    companion object {
        fun alias(x: XStream) {
            val jClass = DivesInteractor::class.java
            x.alias(Aliases.DIVESI, jClass)
            x.aliasAttribute(jClass, "market", "m")
            x.aliasAttribute(jClass, "model", "mo")
        }

        const val BASE_SIZE = 3
        const val BASE_PATHER_INTEREST = 1
        const val UNION_HQ_STABILITY = 1
        const val DIVES_STABILITY = 0
        const val DEFENSE_BONUS = 0.1f
        const val MILITIA_DEFENSE_BONUS = 100f
        const val UNION_MILITIA_DIVISOR = 4
        const val MILITIA_ID = "Militia"
        const val DIVES_RANGE = 1f
        const val HQ_RANGE_BONUS = 5f
        const val PARASITES_PENALTY = -1
        const val MINING_RANGE_CHECK_DAYS_MIN = 9f
        const val MINING_RANGE_CHECK_DAYS_MAX = 11f
        @JvmField
        val MILITARY_INDUSTRIES = mutableListOf(
            Industries.PATROLHQ,
            Industries.MILITARYBASE,
            Industries.HIGHCOMMAND,
            RoiderIndustries.THI_MERCS
        )
        @JvmField
        val HEAVY_INDUSTRIES = mutableListOf(
            Industries.HEAVYINDUSTRY,
            Industries.ORBITALWORKS,
            RoiderIndustries.SHIPWORKS,
            RoiderIndustries.MS_MASS_IND,
            RoiderIndustries.MS_MIL_PROD,
            RoiderIndustries.MS_MODULAR_FAC,
            RoiderIndustries.MS_SHIPYARD,
            RoiderIndustries.XLU_BATTLE_YARDS
        )
        const val MIL_FLAG = "ind_" + RoiderIndustries.UNION_HQ

        fun createBaseCommander(market: MarketAPI): PersonAPI {
            val result: PersonAPI = Helper.roiders?.createRandomPerson()
                ?: throw NullPointerException("Unable to create Union HQ commander at ${market.name}.")
            result.rankId = Ranks.GROUND_GENERAL
            result.postId = RoiderIds.Roider_Ranks.POST_BASE_COMMANDER

            val ip: ImportantPeopleAPI = Helper.sector?.importantPeople ?: return result
            ip.addPerson(result)
            ip.checkOutPerson(result, RoiderIds.PERMANENT_STAFF)

            return result
        }
    }

    private lateinit var market: MarketAPI

    val baseCommander: PersonAPI
        get() {
            return Memory.get(
                "\$${RoiderIds.Roider_Ranks.POST_BASE_COMMANDER}",
                market,
                { it is PersonAPI },
                { createBaseCommander(market) }
            ) as PersonAPI
        }

    private val minerId: String
        get() = market.id + "_" + RoiderIndustries.DIVES

    private var patrolManager: UnionHQPatrolManager? = null
    private val miningRangeCheck = IntervalUtil(MINING_RANGE_CHECK_DAYS_MIN, MINING_RANGE_CHECK_DAYS_MAX)
    private val systemsInRange = mutableListOf<StarSystemAPI>()

    fun init(market: MarketAPI?) {
        this.market = market ?: throw NullPointerException("market should never be null - please report this")
        patrolManager = UnionHQPatrolManager(market)
        updateSystemsInRange()
    }

    fun unapply(modId: String) {
        removeDisposableMinerWeights()
        market.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.unmodify(modId)
        market.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.unmodify(modId + MILITIA_ID)
    }

//    fun getHarvestTargetsInRange(market: MarketAPI?, ly: Float): List<MarketAPI> {
//        if (market == null) return listOf()
//        val targets: MutableList<MarketAPI> = mutableListOf()
//        targets.add(market)
//        val pTargets = mutableListOf<PlanetAPI>()
//        pTargets.addAll(market.starSystem.planets)
//        Helper.sector?.starSystems
//            ?.filter { !Helper.isSpecialSystem(it) }
//            ?.filter { it.id != market.starSystem.id }
//            ?.filter { !it.hasTag(Tags.THEME_UNSAFE) }
//            ?.filter { Misc.getDistanceLY(market.locationInHyperspace, it.location) <= ly }
//            ?.forEach { if (it.planets.isEmpty()) targets.add(it.center.market) else pTargets.addAll(it.planets) }
//
//        pTargets.asSequence().filter { !it.isStar }
//            .filter { !it.memoryWithoutUpdate.getBoolean(MemoryKeys.CLAIMED) }
//            .filter { it.market != null }
//            .filter { it.market != market }
//            .filter { it.market.faction != null }
//            .filter { (it.market.faction.isNeutralFaction && !it.market.isPlanetConditionMarketOnly)
//                    || it.market.factionId == market.factionId }.toList()
//            .forEach { targets.add(it.market) }
//
//        return targets
//    }

    fun applyDisposableMinerWeights() {
        Memory.setFlag(MemoryKeys.MINER_SOURCE, RoiderIndustries.DIVES, market)
        MiningHelper.setMiningDist(minerId, market.primaryEntity, miningRange.modifiedValue)
        var rank = MiningHelper.DIVES_RANK
        var remoteRank = MiningHelper.DIVES_REMOTE_RANK
        var weight = 0f
        var unionWeight = 0f
        if (isUnionHQ()) {
            rank = MiningHelper.UNION_HQ_RANK
            remoteRank = MiningHelper.UNION_HQ_REMOTE_RANK
            weight += MiningHelper.WEIGHT_MID
            unionWeight = MiningHelper.WEIGHT_HIGH
        }
        if (market.factionId == RoiderFactions.ROIDER_UNION) {
            rank++
            weight += unionWeight
            unionWeight = 0f
        }
        val roiderWeight = if (market.factionId != RoiderFactions.ROIDER_UNION) unionWeight else 0f
        val weights = mapOf(
            kotlin.Pair(Factions.INDEPENDENT, MiningHelper.WEIGHT_MIN),
            kotlin.Pair(Factions.PIRATES, MiningHelper.WEIGHT_MIN),
            kotlin.Pair(RoiderFactions.ROIDER_UNION, roiderWeight),
            kotlin.Pair(market.factionId, weight)
        )
        if (market.containingLocation == null) return
        applyDisposableMinerWeights(market.containingLocation, rank, weights)
        systemsInRange.asSequence()
            .filter { MiningHelper.inMiningRange(it, market.primaryEntity) }
            .filter { MiningHelper.canMine(it) }
            .filterNot { it === market.containingLocation }
            .forEach { applyDisposableMinerWeights(it, remoteRank, weights) }
    }

    fun applyDisposableMinerWeights(target: LocationAPI, rank: Float, weights: Map<String, Float>) {
        MiningHelper.setMiningRank(minerId, target, rank)
        MiningHelper.setMiningWeights(
            minerId,
            target,
            *weights.map { kotlin.Pair(it.key, it.value) }.toTypedArray()
        )
    }

    fun applyGroundDefense(
        getDeficitMult: (Array<String>) -> Float,
        getMaxDeficit: (Array<String>) -> Pair<String, Int>,
        nameForModifier: String,
        modId: String
    ) {
        model.militiaEnabled = !market.faction.illegalCommodities.contains(Commodities.HAND_WEAPONS)
        if (model.militiaEnabled) {
            applyMilitiaDefense(nameForModifier, modId)
        }

        if (isUnionHQ()) {
            val mult: Float = getDeficitMult(arrayOf(Commodities.SUPPLIES))
            val bonus = 1f + DEFENSE_BONUS * mult
            model.defenseBonus = bonus
            val postfix = if (mult != 1f) {
                val com: String = getMaxDeficit(arrayOf(Commodities.SUPPLIES)).one
                " (" + BaseIndustry.getDeficitText(com).lowercase(Locale.getDefault()) + ")" //extern?
            } else {
                ""
            }
            market.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)
                ?.modifyMult(modId, bonus, nameForModifier + postfix)
        }
    }

    private fun applyMilitiaDefense(nameForModifier: String, modId: String) {
        val bonus = if (isUnionHQ()) MILITIA_DEFENSE_BONUS / UNION_MILITIA_DIVISOR * market.size
            else MILITIA_DEFENSE_BONUS
        model.militiaStrength = bonus
        market.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)
            ?.modifyFlat(modId + MILITIA_ID, bonus, nameForModifier)
    }

    fun isFunctional(functional: Boolean, canMine: Boolean): Boolean {
        val hostileToRoiders = market.faction?.isHostileTo(RoiderFactions.ROIDER_UNION) == true
        return if (isUnionHQ()) functional && !hostileToRoiders
        else functional and canMine
    }

    fun advance(amount: Float, isFunctional: Boolean) {
        if (!market.isInEconomy) return
        if (Helper.sector?.economy?.isSimMode == true) return
        if (isUnionHQ()) {
            patrolManager?.advance(amount, isFunctional)
        }
        miningRangeCheck.advance(Misc.getDays(amount))
        if (!miningRangeCheck.intervalElapsed()) return
        updateSystemsInRange()
    }

    fun updateSystemsInRange() {
        if (isUnionHQ()) miningRange.modifyFlat(RoiderIndustries.UNION_HQ, HQ_RANGE_BONUS)
        if (!this::market.isInitialized) return
        systemsInRange.clear()
        val location = market.locationInHyperspace
        for (s in Helper.sector?.starSystems ?: listOf()) {
            if (Misc.getDistanceLY(s.location, location) <= miningRange.modifiedValue) {
                systemsInRange += s
            }
        }
    }

    fun selectNameForModifier(spec: IndustrySpecAPI) {
        model.isUnionHQ = isUnionHQ()
        model.nameForModifier = if (isUnionHQ()) spec.name else Misc.ucFirst(spec.name)
    }

    fun selectCurrentImage(currentImage: String) {
        model.image = if (isUnionHQ() && marketHasHeavyIndustry()) {
            Helper.settings?.getSpriteName(Categories.INDUSTRY, RoiderIndustries.SPRITE_UNION_HQ_HEAVY) ?: ""
        } else {
            currentImage
        }
    }

    private fun marketHasHeavyIndustry(): Boolean {
        if (HEAVY_INDUSTRIES.any { market.hasIndustry(it) }) return true
        return market.industries?.any { it.spec.hasTag(RoiderTags.HEAVY_INDUSTRY) } == true
    }

    fun selectPostDescription(mode: Industry.IndustryTooltipMode, isFunctional: Boolean) {
        model.isUnionHQ = isUnionHQ()
        model.isHostile = market.faction?.isHostileTo(RoiderFactions.ROIDER_UNION) == true
        model.showPostDescription = mode == Industry.IndustryTooltipMode.NORMAL
        if (!model.showPostDescription) return
        model.isFunctional = isFunctional
        model.isMilitary = MILITARY_INDUSTRIES.none { market.hasIndustry(it) }
        model.isFullConversions = marketHasHeavyIndustry()
        model.marketName = market.name
        model.theMarketFaction = market.faction?.displayNameWithArticle ?: ExternalStrings.DEBUG_NULL
    }

    fun selectUnavailableReason(unavailableReason: String) {
        model.isUnionHQ = isUnionHQ()
        model.isDivesBpKnown = MarketHelper.playerGovernorOrMarketFactionKnowsIndustry(RoiderIndustries.DIVES, market)
        model.isHqBpKnown = MarketHelper.playerGovernorOrMarketFactionKnowsIndustry(RoiderIndustries.UNION_HQ, market)
        model.defaultUnavailableReason = unavailableReason
    }

    private fun removeDisposableMinerWeights() {
        systemsInRange.forEach {
            MiningHelper.unsetMiningRank(minerId, it)
            MiningHelper.unsetMiningWeights(minerId, it)
        }
        Memory.unsetFlag(MemoryKeys.MINER_SOURCE, RoiderIndustries.DIVES, market)
        MiningHelper.unsetMiningDist(RoiderIndustries.DIVES, market.primaryEntity)
    }

    fun getBaseStabilityMod(): Int {
        return if (isUnionHQ()) UNION_HQ_STABILITY else DIVES_STABILITY
    }

    fun addSubmarketIfNeeded(functional: Boolean) {
        if (functional && isUnionHQ() && !market.hasSubmarket(RoiderIds.Roider_Submarkets.UNION_MARKET)) {
            market.addSubmarket(RoiderIds.Roider_Submarkets.UNION_MARKET)
        }
    }

    fun buildingFinished(isFunctional: Boolean) {
        if (isUnionHQ()) finishUnionHQ(isFunctional)
    }

    fun canUpgrade(): Boolean {
        return MarketHelper.playerGovernorOrMarketFactionKnowsIndustry(RoiderIndustries.UNION_HQ, market)
    }

    fun upgradeFinished(isFunctional: Boolean) {
        finishUnionHQ(isFunctional)
    }

    private fun finishUnionHQ(isFunctional: Boolean) {
        Memory.setFlag(MemFlags.MARKET_MILITARY, MIL_FLAG, market)
        miningRange.modifyFlat(RoiderIndustries.UNION_HQ, HQ_RANGE_BONUS)
        addSubmarketIfNeeded(isFunctional)
        addBaseCommander()
        patrolManager?.tracker?.forceIntervalElapsed()
    }

    fun downgrade() {
        miningRange.unmodifyFlat(RoiderIndustries.UNION_HQ)
        market.removeSubmarket(RoiderIds.Roider_Submarkets.UNION_MARKET)
        patrolManager?.tracker?.forceIntervalElapsed()
        Memory.unsetFlag(MemFlags.MARKET_MILITARY, MIL_FLAG, market)
        removeBaseCommander()
    }

    fun notifyDisrupted() {
        removeBaseCommander()
    }

    fun disruptionFinished() {
        patrolManager?.tracker?.forceIntervalElapsed()
        addBaseCommander()
    }

    fun notifyBeingRemoved(forUpgrade: Boolean) {
        if (forUpgrade) return
        Memory.unsetFlag(MemFlags.MARKET_MILITARY, MIL_FLAG, market)
        market.removeSubmarket(RoiderIds.Roider_Submarkets.UNION_MARKET)
        removeBaseCommander()
        patrolManager = null
    }

    private fun addBaseCommander() {
        market.commDirectory?.addPerson(baseCommander)
        market.addPerson(baseCommander)
        Helper.sector?.importantPeople?.getData(baseCommander)?.location?.market = market
    }

    private fun removeBaseCommander() {
        market.commDirectory?.removePerson(baseCommander)
        market.removePerson(baseCommander)
        Helper.sector?.importantPeople?.getData(baseCommander)?.location?.market = null
    }

    fun hasPostDemandSection(mode: Industry.IndustryTooltipMode?, isFunctional: Boolean): Boolean {
        return mode != Industry.IndustryTooltipMode.NORMAL || isFunctional
    }

    fun selectPostDemand(getMaxDeficit: (Array<String>) -> Pair<String, Int>) {
        val deficit = getMaxDeficit(arrayOf(Commodities.DRUGS))
        model.isDrugsDeficit = deficit.two > 0
        model.drugsDeficit = deficit.two.toString()
    }

    fun isAvailableToBuild(superAvailable: Boolean): Boolean {
        val faction = if (MarketHelper.isNexerelinGovernorship(market)) Helper.sector?.playerFaction ?: return false
            else market.faction ?: return false
        val bpKnown = faction.knowsIndustry(RoiderIndustries.DIVES)
        return if (isUnionHQ()) faction.knowsIndustry(RoiderIndustries.UNION_HQ) && superAvailable
        else superAvailable && bpKnown && market.hasSpaceport()
    }

    fun showWhenUnavailable(): Boolean {
        return if (market.hasTag(Tags.MARKET_NO_INDUSTRIES_ALLOWED)) false
        else if (market.faction?.knowsIndustry(RoiderIndustries.DIVES) == true) true
        else if (Helper.hasCommission(RoiderFactions.ROIDER_UNION)) true
        else Helper.sector?.playerFaction?.knowsIndustry(RoiderIndustries.DIVES) == true
    }

    fun getPatherInterest(patherInterest: Float): Float = patherInterest + BASE_PATHER_INTEREST

    fun modifyIncoming(
        modId: String,
        market: MarketAPI?,
        incoming: PopulationComposition?,
        drugsShortageDesc: String,
        getMaxDeficit: (Array<String>) -> Pair<String, Int>
    ) {
        if (Helper.anyNull(market, incoming)) return

        val deficit: Pair<String, Int> = getMaxDeficit(arrayOf(Commodities.DRUGS))
        if (deficit.two > 0) {
            incoming!!.weight.modifyFlat(modId, -deficit.two.toFloat(), drugsShortageDesc)
        }
    }

    fun canImprove(): Boolean = true
}