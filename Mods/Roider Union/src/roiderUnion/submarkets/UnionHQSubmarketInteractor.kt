package roiderUnion.submarkets

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BlueprintProviderItem
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.procgen.DropGroupRow
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.AllianceManager
import exerelin.campaign.PlayerFactionStore
import retroLib.RetrofitsKeeper
import retroLib.impl.BaseRetrofitAdjuster
import roiderUnion.ModPlugin
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Settings
import roiderUnion.ids.*
import roiderUnion.retrofits.UnionHQFilter
import java.util.*
import kotlin.math.max

class UnionHQSubmarketInteractor(private val model: UnionHQSubmarketModel) {
    companion object {
        const val BASE_COMBAT = 200f
        const val BASE_FREIGHTER = 15f
        const val BASE_TANKER = 10f
        const val BASE_TRANSPORT = 20f
        const val BASE_LINER = 10f
        const val BASE_UTILITY = 10f

        val UNION_HQ_BP_REP = RepLevel.WELCOMING.ordinal - Helper.REP_LEVEL_ORDINAL_OFFSET
        val SHIPWORKS_BP_REP = RepLevel.FRIENDLY.ordinal - Helper.REP_LEVEL_ORDINAL_OFFSET
    }

    @Transient
    private lateinit var reqReps: MutableMap<String, RepLevel>

    @Transient
    private lateinit var reqCom: MutableMap<String, Boolean>
    
    fun advance(submarket: SubmarketAPI) {
        if (submarket.faction?.id != RoiderFactions.ROIDER_UNION) {
            submarket.faction = Helper.roiders ?: return
        }
    }

    fun init(submarket: SubmarketAPI) {
        loadShipRequirements(submarket.market)
    }
    private fun loadShipRequirements(market: MarketAPI) {
        reqReps = mutableMapOf()
        reqCom = mutableMapOf()
        loadRetrofitData(market)
        loadOverrides()
    }
    
    private fun loadRetrofitData(market: MarketAPI) {
        val retrofits = RetrofitsKeeper.getRetrofits(UnionHQFilter(market, BaseRetrofitAdjuster(market)))
        for (data in retrofits) {
            val isComKnown = reqCom.containsKey(data.target)
            val requiresCommission = reqCom[data.target] == true
            if (!isComKnown || requiresCommission) {
                reqCom[data.target] = data.commission
            }

            val isRepKnown = reqReps.containsKey(data.target)
            val reputationIsTooHigh = reqReps[data.target]?.isAtWorst(data.reputation) == true
            if (!isRepKnown || reputationIsTooHigh) {
                reqReps[data.target] = data.reputation
            }
        }
    }
    
    private fun loadOverrides() {
        val reps = Settings.UNION_HQ_SHIP_REP_REQ
        for (ship in reps.keys) {
            reqReps[ship] = Helper.toRepLevel(reps[ship] ?: 0f)
        }
        for (ship in Settings.UNION_HQ_SHIP_COM_NOT_REQ) {
            reqCom[ship] = false
        }
    }

    fun updateCargoPrePlayerInteraction(
        seconds: Float,
        addAndRemoveStockpiledResources: (Float, Boolean, Boolean, Boolean) -> Unit,
        okToUpdateShipsAndWeapons: () -> Boolean,
        swUpdate: (Float) -> Unit,
        pruneWeapons: (Float) -> Unit,
        submarket: SubmarketAPI,
        addWeapons: (Int, Int, Int, String?) -> Unit,
        addFighters: (Int, Int, Int, String?) -> Unit,
        cargo: CargoAPI?,
        addShips: (String?, Float, Float, Float, Float, Float, Float, Float?, Float, FactionAPI.ShipPickMode?, FactionDoctrineAPI?) -> Unit,
        itemGenRandom: Random,
        addHullMods: (Int, Int) -> Unit
    ) {
        addAndRemoveStockpiledResources(seconds, false, true, true)
        if (okToUpdateShipsAndWeapons()) {
            val marketSize = submarket.market?.size ?: 3
            loadShipRequirements(submarket.market)
            swUpdate(0f)
            pruneWeapons(0f)
            val weapons = 4 + max(0, marketSize - 3) * 2 // extern
            val fighters = 2 + max(0, marketSize - 3)
            addWeapons(weapons, weapons + 2, 3, submarket.faction?.id)
            addFighters(fighters, fighters + 2, 3, submarket.faction?.id)
            val stability = submarket.market.stabilityValue
            val sMult = max(0.1f, stability / 10f)
            cargo?.mothballedShips?.clear()
            val doctrineOverride = submarket.faction.doctrine.clone()
            var size = doctrineOverride.shipSize + 1
            if (stability <= 4) size++
            if (stability <= 6) size++
            doctrineOverride.shipSize = size.coerceAtMost(5)
            addShips(
                submarket.faction?.id,
                BASE_COMBAT * sMult,
                BASE_FREIGHTER,
                BASE_TANKER,
                BASE_TRANSPORT,
                BASE_LINER,
                BASE_UTILITY,
                null,
                0f,
                null,
                doctrineOverride
            )
            addHullMods(4, 2 + itemGenRandom.nextInt(4))
            addBlueprints(1 + itemGenRandom.nextInt(2), cargo, itemGenRandom)
        }

        cargo?.removeItems(
            CargoAPI.CargoItemType.SPECIAL,
            SpecialItemData(Items.INDUSTRY_BP, RoiderIndustries.UNION_HQ),
            1f
        )
        cargo?.removeItems(
            CargoAPI.CargoItemType.SPECIAL,
            SpecialItemData(RoiderItems.UNION_HQ_BP, RoiderIndustries.UNION_HQ),
            1f
        )
        cargo?.removeItems(
            CargoAPI.CargoItemType.SPECIAL,
            SpecialItemData(Items.INDUSTRY_BP, RoiderIndustries.SHIPWORKS),
            1f
        )
        cargo?.removeItems(
            CargoAPI.CargoItemType.SPECIAL,
            SpecialItemData(RoiderItems.SHIPWORKS_BP, RoiderIndustries.SHIPWORKS),
            1f
        )
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
        if (Helper.sector?.playerFaction?.knowsIndustry(RoiderIndustries.UNION_HQ) == false) {
            cargo?.addSpecial(SpecialItemData(RoiderItems.UNION_HQ_BP, RoiderIndustries.UNION_HQ), 1f)
        }
        if (Helper.sector?.playerFaction?.knowsIndustry(RoiderIndustries.SHIPWORKS) == false) {
            cargo?.addSpecial(SpecialItemData(RoiderItems.SHIPWORKS_BP, RoiderIndustries.SHIPWORKS), 1f)
        }
        if (Helper.sector?.playerFaction?.knowsIndustry(RoiderIndustries.DIVES) == false) {
            cargo?.addSpecial(SpecialItemData(RoiderItems.DIVES_BP, RoiderIndustries.DIVES), 1f)
        }
        cargo?.sort()
    }

    private fun addBlueprints(num: Int, cargo: CargoAPI?, itemGenRandom: Random) {
        for (stack in cargo?.stacksCopy ?: listOf()) {
            if (stack.isSpecialStack && (isSpecialId(RoiderItems.RETROFIT_BP, stack)
                        || isSpecialId(RoiderTags.ROIDER_BP, stack))
            ) {
                cargo?.removeStack(stack)
            }
        }
        val picker = DropGroupRow.getPicker(RoiderIds.DropGroups.MARKET_BPS)
        picker.random = itemGenRandom
        var added = 0
        var tries = 0
        val alreadyAdded = mutableSetOf<String>()
        while (added < num) {
            tries++
            if (tries > num * 3) break
            var pick: DropGroupRow = picker.pick()
            if (pick.isMultiValued) {
                pick = pick.resolveToSpecificItem(itemGenRandom)
            }
            if (pick.isNothing) continue
            val dataId: String = pick.specialItemData ?: continue
            if (Helper.sector?.playerFaction?.knowsShip(dataId) == true) continue
            if (Helper.sector?.playerFaction?.knowsFighter(dataId) == true) continue
            if (Helper.sector?.playerFaction?.knowsWeapon(dataId) == true) continue
            if (dataId == RoiderItems.ROIDER_PACKAGE) {
                val bpi: BlueprintProviderItem =
                    pick.specialItemSpec.getNewPluginInstance(null) as BlueprintProviderItem
                if (allBPsKnown(bpi)) continue
            }
            if (dataId.isEmpty() && alreadyAdded.contains(pick.specialItemId)) continue
            else if (alreadyAdded.contains(dataId)) continue
            val data = SpecialItemData(pick.specialItemId, dataId)
            cargo?.addItems(CargoAPI.CargoItemType.SPECIAL, data, 1f)
            added++
            alreadyAdded.add(dataId)
        }
    }

    private fun isSpecialId(id: String, stack: CargoStackAPI): Boolean {
        return stack.specialItemSpecIfSpecial.id == id
    }

    private fun allBPsKnown(bpi: BlueprintProviderItem): Boolean {
        val faction: FactionAPI = Helper.sector?.playerFaction ?: return false
        if (bpi.providedFighters != null) {
            for (id in bpi.providedFighters) {
                if (!faction.knowsFighter(id)) return false
            }
        }
        if (bpi.providedWeapons != null) {
            for (id in bpi.providedWeapons) {
                if (!faction.knowsWeapon(id)) return false
            }
        }
        if (bpi.providedShips != null) {
            for (id in bpi.providedShips) {
                if (!faction.knowsShip(id)) return false
            }
        }
        if (bpi.providedIndustries != null) {
            for (id in bpi.providedIndustries) {
                if (!faction.knowsIndustry(id)) return false
            }
        }
        return true
    }

    fun hasCommission(factionId: String?): Boolean {
        if (ModPlugin.hasNexerelin) {
            val commissionFaction: String? = Misc.getCommissionFactionId()
            if (commissionFaction != null && AllianceManager.areFactionsAllied(
                    commissionFaction,
                    RoiderFactions.ROIDER_UNION
                )
            ) {
                return true
            }
            if (AllianceManager.areFactionsAllied(
                    PlayerFactionStore.getPlayerFactionId(),
                    RoiderFactions.ROIDER_UNION
                )
            ) {
                return true
            }
        }
        return factionId == Misc.getCommissionFactionId()
    }

    fun requiresCommission(req: RepLevel?): Boolean {
        return req?.isAtWorst(RepLevel.WELCOMING) == true
    }

    /**
     * @return true or null
     */
    fun isRoiderResource(com: CommodityOnMarketAPI?): Boolean? {
        return when (com?.id ?: "") {
            Commodities.ORE -> true
            Commodities.RARE_ORE -> true
            Commodities.VOLATILES -> true
            Commodities.ORGANICS -> true
            else -> null
        }
    }

    fun isIllegalOnSubmarket(submarket: SubmarketAPI, commodityId: String, action: TransferAction): Boolean {
        val illegal = submarket.faction.isIllegal(commodityId)
        val req = getRequiredLevelAssumingLegal(submarket, commodityId, action) ?: return illegal
        val level = Helper.roiders?.getRelationshipLevel(Helper.sector?.playerFaction) ?: return illegal
        var legal = level.isAtWorst(req)
        if (requiresCommission(req)) {
            legal = legal and hasCommission(submarket.faction.id)
        }
        return !legal
    }

    private fun getRequiredLevelAssumingLegal(submarket: SubmarketAPI, commodityId: String, action: TransferAction): RepLevel? {
        if (action == TransferAction.PLAYER_SELL) return RepLevel.VENGEFUL
        val com = submarket.market?.getCommodityData(commodityId) ?: return null
        val isMilitary = com.commodity?.tags?.contains(Commodities.TAG_MILITARY) == true
        return if (isMilitary) {
            if (com.isPersonnel) RepLevel.COOPERATIVE
            else RepLevel.FAVORABLE
        } else {
            null
        }
    }

    fun isIllegalOnSubmarket(submarket: SubmarketAPI, stack: CargoStackAPI, action: TransferAction): Boolean {
        if (stack.isCommodityStack) {
            return isIllegalOnSubmarket(submarket, stack.data as String, action)
        }
        val req = getRequiredLevelAssumingLegal(submarket, stack, action) ?: return false
        val level = Helper.roiders?.getRelationshipLevel(Helper.sector?.playerFaction) ?: return false
        var legal = level.isAtWorst(req)
        if (requiresCommission(req)) {
            legal = legal and hasCommission(submarket.faction.id)
        }
        return !legal
    }

    private fun getRequiredLevelAssumingLegal(submarket: SubmarketAPI, stack: CargoStackAPI, action: TransferAction): RepLevel? {
        var tier = -1
        if (stack.isWeaponStack) {
            val spec: WeaponSpecAPI = stack.weaponSpecIfWeapon
            tier = spec.tier
        } else if (stack.isSpecialStack && stack.specialDataIfSpecial.id == Items.TAG_MODSPEC) {
            val spec: HullModSpecAPI = stack.hullModSpecIfHullMod
            tier = spec.tier
        } else if (stack.isFighterWingStack) {
            val spec: FighterWingSpecAPI = stack.fighterWingSpecIfWing
            tier = spec.tier
        } else if (stack.isSpecialStack) {
            val data: SpecialItemData = stack.specialDataIfSpecial
            when (data.id) {
                RoiderItems.UNION_HQ_BP -> tier = UNION_HQ_BP_REP
                RoiderItems.SHIPWORKS_BP -> tier = SHIPWORKS_BP_REP
                RoiderItems.RETROFIT_BP -> tier = (getRequiredLevelAssumingLegal(data.data, action)?.ordinal
                    ?: Helper.REP_LEVEL_ORDINAL_OFFSET) - Helper.REP_LEVEL_ORDINAL_OFFSET
                Items.FIGHTER_BP -> {
                    val fSpec = Helper.settings?.getFighterWingSpec(data.data)
                    if (fSpec?.hasTag(RoiderTags.ROIDER) == true) tier = fSpec.tier
                }

                Items.WEAPON_BP -> {
                    val wSpec = Helper.settings?.getWeaponSpec(data.data)
                    if (wSpec?.hasTag(RoiderTags.ROIDER) == true) tier = wSpec.tier
                }
            }
        }
        if (tier >= 0) {
            if (action == TransferAction.PLAYER_BUY) {
                when (tier) {
                    0 -> return RepLevel.FAVORABLE
                    1 -> return RepLevel.WELCOMING
                    2 -> return RepLevel.FRIENDLY
                    3 -> return RepLevel.COOPERATIVE
                }
            }
            return RepLevel.VENGEFUL
        }
        return if (!stack.isCommodityStack) null
        else getRequiredLevelAssumingLegal(submarket, stack.data as String, action)
    }

    fun selectIllegalTransferText(submarket: SubmarketAPI, stack: CargoStackAPI, action: TransferAction) {
        val req = getRequiredLevelAssumingLegal(submarket, stack, action)
        model.commissionRequired = requiresCommission(req)
        model.reqName = req?.displayName ?: ExternalStrings.DEBUG_NULL
        model.factionName = submarket.faction?.displayName ?: ExternalStrings.DEBUG_NULL
    }

    fun isIllegalOnSubmarket(factionId: String?, member: FleetMemberAPI, action: TransferAction): Boolean {
        val req = getRequiredLevelAssumingLegal(member, action) ?: return false
        val level = Helper.roiders?.getRelationshipLevel(Helper.sector?.playerFaction) ?: return false
        var legal = level.isAtWorst(req)
        if (requiresCommission(member, req)) {
            legal = legal and hasCommission(factionId)
        }
        return !legal
    }

    private fun getRequiredLevelAssumingLegal(
        member: FleetMemberAPI,
        action: TransferAction
    ): RepLevel? {
        return getRequiredLevelAssumingLegal(member.hullSpec?.baseHullId ?: "", action)
    }

    private fun getRequiredLevelAssumingLegal(
        hullId: String,
        action: TransferAction
    ): RepLevel? {
        if (!this::reqReps.isInitialized) {
            Global.getLogger(this::class.java).error("Roider Union HQ submarket's reputation requirements not initialized.")
            reqReps = mutableMapOf()
        }
        if (action == TransferAction.PLAYER_BUY) {
            if (reqReps.containsKey(hullId)) {
                return reqReps[hullId]
            }
            val fp: Int = Helper.settings?.getHullSpec(hullId)?.fleetPoints ?: 0
            val size: ShipAPI.HullSize = Helper.settings?.getHullSpec(hullId)?.hullSize ?: ShipAPI.HullSize.FRIGATE
            if (size == ShipAPI.HullSize.CAPITAL_SHIP || fp > 15) return RepLevel.COOPERATIVE
            if (size == ShipAPI.HullSize.CRUISER || fp > 10) return RepLevel.FRIENDLY
            return if (size == ShipAPI.HullSize.DESTROYER || fp > 5) RepLevel.WELCOMING
            else RepLevel.FAVORABLE
        }
        return null
    }

    private fun requiresCommission(member: FleetMemberAPI, req: RepLevel): Boolean {
        if (!this::reqCom.isInitialized) {
            Global.getLogger(this::class.java).error("Roider Union HQ submarket's commission requirements not initialized.")
            reqCom = mutableMapOf()
        }
        return if (reqCom.containsKey(member.hullSpec.baseHullId)) reqCom[member.hullSpec.baseHullId]!!
        else requiresCommission(req)
    }

    fun selectIllegalTransferText(submarket: SubmarketAPI, member: FleetMemberAPI, action: TransferAction) {
        val req = getRequiredLevelAssumingLegal(member, action)
        model.commissionRequired = requiresCommission(member, req ?: RepLevel.VENGEFUL)
        model.reqName = req?.displayName ?: ExternalStrings.DEBUG_NULL
        model.factionName = submarket.faction?.displayName ?: ExternalStrings.DEBUG_NULL
    }

    fun isEnabled(market: MarketAPI): Boolean = market.hasFunctionalIndustry(RoiderIndustries.UNION_HQ)

    fun selectTooltipAppendix(ui: CoreUIAPI?, submarket: SubmarketAPI, enabled: Boolean) {
        if (!isEnabled(submarket.market)) {
            model.tooltipAppendix = UnionHQTooltipAppendix.NOT_FUNCTIONAL
            return
        }
        if (!enabled) {
            model.factionName = submarket.faction?.displayName ?: ExternalStrings.DEBUG_NULL
            model.tooltipAppendix = UnionHQTooltipAppendix.LOW_REP
            return
        }
        if (ui?.tradeMode == CampaignUIAPI.CoreUITradeMode.SNEAK) {
            model.tooltipAppendix = UnionHQTooltipAppendix.SNEAK
            return
        }

        model.tooltipAppendix = UnionHQTooltipAppendix.OTHER
    }
}
