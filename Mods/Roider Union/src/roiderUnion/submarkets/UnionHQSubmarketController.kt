package roiderUnion.submarkets

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.submarkets.MilitarySubmarketPlugin
import com.fs.starfarer.api.util.Highlights
import roiderUnion.helpers.Helper

class UnionHQSubmarketController : MilitarySubmarketPlugin() {
    private val model: UnionHQSubmarketModel
    private val interactor: UnionHQSubmarketInteractor
    private val view: UnionHQSubmarketView

    init {
        model = UnionHQSubmarketModel()
        interactor = UnionHQSubmarketInteractor(model)
        view = UnionHQSubmarketView(model)
    }

    override fun init(submarket: SubmarketAPI) {
        super.init(submarket)
        interactor.init(submarket)
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        interactor.advance(submarket)
    }

    override fun updateCargoPrePlayerInteraction() {
        val seconds = Helper.sector?.clock?.convertToSeconds(sinceLastCargoUpdate) ?: 0f
        sinceLastCargoUpdate = 0f
        interactor.updateCargoPrePlayerInteraction(
            seconds,
            this::addAndRemoveStockpiledResources,
            this::okToUpdateShipsAndWeapons,
            this::swUpdate,
            this::pruneWeapons,
            submarket,
            this::addWeapons,
            this::addFighters,
            cargo,
            this::addShips,
            itemGenRandom,
            this::addHullMods
        )
    }

    private fun swUpdate(value: Float) {
        sinceSWUpdate = value
    }

    override fun hasCommission(): Boolean {
        return interactor.hasCommission(market?.factionId)
    }

    override fun requiresCommission(req: RepLevel?): Boolean {
        return interactor.requiresCommission(req)
    }

    override fun getName(): String {
        return view.getName()
    }

    override fun shouldHaveCommodity(com: CommodityOnMarketAPI?): Boolean {
        return interactor.isRoiderResource(com) ?: super.shouldHaveCommodity(com)
    }

    override fun isIllegalOnSubmarket(commodityId: String?, action: TransferAction?): Boolean {
        if (Helper.anyNull(commodityId, action)) return false
        return interactor.isIllegalOnSubmarket(submarket, commodityId!!, action!!)
    }

    override fun isIllegalOnSubmarket(stack: CargoStackAPI?, action: TransferAction?): Boolean {
        if (Helper.anyNull(stack, action)) return false
        return interactor.isIllegalOnSubmarket(submarket, stack!!, action!!)
    }

    override fun getIllegalTransferText(stack: CargoStackAPI?, action: TransferAction?): String {
        if (Helper.anyNull(stack, action)) return view.getDefaultIllegalTransferText()
        interactor.selectIllegalTransferText(submarket, stack!!, action!!)
        return view.getIllegalTransferText(stack)
    }

    override fun getIllegalTransferTextHighlights(
        stack: CargoStackAPI?,
        action: TransferAction?
    ): Highlights? {
        if (Helper.anyNull(stack, action)) return null
        interactor.selectIllegalTransferText(submarket, stack!!, action!!)
        return view.getIllegalTransferTextHighlights()
    }

    override fun isIllegalOnSubmarket(member: FleetMemberAPI?, action: TransferAction?): Boolean {
        if (Helper.anyNull(member, action)) return false
        interactor.init(submarket)
        return interactor.isIllegalOnSubmarket(submarket.faction?.id, member!!, action!!)
    }

    override fun getIllegalTransferText(member: FleetMemberAPI?, action: TransferAction?): String {
        if (Helper.anyNull(member, action)) return view.getDefaultIllegalTransferText()
        interactor.init(submarket)
        interactor.selectIllegalTransferText(submarket, member!!, action!!)
        return view.getIllegalShipTransferText()
    }

    override fun getIllegalTransferTextHighlights(member: FleetMemberAPI?, action: TransferAction?): Highlights? {
        if (Helper.anyNull(member, action)) return null
        interactor.init(submarket)
        interactor.selectIllegalTransferText(submarket, member!!, action!!)
        return view.getIllegalShipTransferTextHighlights()
    }

    override fun isEnabled(ui: CoreUIAPI?): Boolean {
        return if (!interactor.isEnabled(market)) false
        else super.isEnabled(ui)
    }

    override fun getTooltipAppendix(ui: CoreUIAPI?): String? {
        interactor.selectTooltipAppendix(ui, submarket, isEnabled(ui))
        return view.getTooltipAppendix()
    }
}