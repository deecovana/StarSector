package roiderUnion.econ

import com.fs.starfarer.api.campaign.econ.*
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderTags

class DivesController : BaseIndustry(), MarketImmigrationModifier {
    companion object {
        fun alias(x: XStream) {
            val jClass = DivesController::class.java
            x.alias(Aliases.DIVESC, jClass)
            x.aliasAttribute(jClass, "model", "rM")
            x.aliasAttribute(jClass, "view", "rV")
            x.aliasAttribute(jClass, "interactor", "rI")
            x.aliasAttribute(jClass, "supplier", "rS")
            DivesModel.alias(x)
            DivesView.alias(x)
            DivesInteractor.alias(x)
            DivesSupplyInteractor.alias(x)
        }
    }

    private val model = DivesModel()
    private val view = DivesView(model)
    val miningRange = MutableStat(DivesInteractor.DIVES_RANGE)
    private val interactor = DivesInteractor(
        model,
        this::isUnionHQ,
        miningRange
    )
    private val supplier = DivesSupplyInteractor(
        model,
        this::supply,
        this::supply,
        this::demand,
        this::getRemoteMiningDesc,
        this::getRemoteMiningBonusDesc,
        this::isUnionHQ,
        miningRange
    )

    private val isUnionHQ: Boolean
        get() = spec.hasTag(RoiderTags.UNION_HQ)

    override fun readResolve(): Any {
        super.readResolve()
        interactor.isUnionHQ = this::isUnionHQ
        interactor.updateSystemsInRange()
        supplier.supply = this::supply
        supplier.supply2 = this::supply
        supplier.demand = this::demand
        supplier.getRemoteMiningDesc = this::getRemoteMiningDesc
        supplier.getRemoteMiningBonusDesc = this::getRemoteMiningBonusDesc
        supplier.isUnionHQ = this::isUnionHQ
        supplier.updateSystemsInRange()
        return this
    }

    override fun init(id: String?, market: MarketAPI?) {
        super.init(id, market)
        supplier.init(market)
        interactor.init(market)
        supplier.calculateRemoteResources()
    }

    override fun apply() {
        apply(true)
        modifyStabilityWithBaseMod()
        supplier.supplyRemoteResources(view.getSporesDesc())
        supplier.applySupplyAndDemand(this::getMaxDeficit, this::applyDeficitToProduction)
        interactor.addSubmarketIfNeeded(isFunctional)
        interactor.applyDisposableMinerWeights()
        interactor.applyGroundDefense(this::getDeficitMult, this::getMaxDeficit, nameForModifier, modId)
        if (!isFunctional) {
            supply.clear()
            unapply()
        }
    }
    override fun unapply() {
        super.unapply()
        unmodifyStabilityWithBaseMod()
        supply.clear()
        interactor.unapply(modId)
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        supplier.advance(amount)
        interactor.advance(amount, isFunctional)
    }

    override fun isFunctional(): Boolean {
        return interactor.isFunctional(super.isFunctional(), supplier.canMine())
    }

    override fun getCurrentImage(): String {
        interactor.selectCurrentImage(super.getCurrentImage())
        return view.image
    }

    override fun addRightAfterDescriptionSection(tooltip: TooltipMakerAPI?, mode: Industry.IndustryTooltipMode?) {
        if (Helper.anyNull(tooltip, mode)) return
        supplier.selectPostDescription()
        interactor.selectPostDescription(mode!!, isFunctional)
        view.showPostDescription(tooltip!!)
    }

    override fun hasPostDemandSection(hasDemand: Boolean, mode: Industry.IndustryTooltipMode?): Boolean {
        return interactor.hasPostDemandSection(mode, isFunctional)
    }

    override fun addPostDemandSection(
        tooltip: TooltipMakerAPI?,
        hasDemand: Boolean,
        mode: Industry.IndustryTooltipMode?
    ) {
        if (Helper.anyNull(tooltip, mode)) return
        interactor.selectPostDemand(this::getMaxDeficit)
        return view.showPostDemandSection(tooltip!!, hasDemand, mode!!, this::addStabilityPostDemandSection)
    }

    override fun getNameForModifier(): String {
        interactor.selectNameForModifier(spec)
        return view.getNameForModifier()
    }

    override fun isAvailableToBuild(): Boolean {
        return interactor.isAvailableToBuild(super.isAvailableToBuild())
    }

    override fun showWhenUnavailable(): Boolean {
        return interactor.showWhenUnavailable()
    }

    override fun getUnavailableReason(): String {
        interactor.selectUnavailableReason(super.getUnavailableReason())
        return view.getUnavailableReason()
    }

    override fun modifyIncoming(market: MarketAPI?, incoming: PopulationComposition?) {
        interactor.modifyIncoming(modId, market, incoming, view.getDrugsShortageDesc(), this::getMaxDeficit)
    }

    override fun getBaseStabilityMod(): Int = interactor.getBaseStabilityMod()
    override fun getPatherInterest(): Float = interactor.getPatherInterest(super.getPatherInterest())

    override fun buildingFinished() {
        super.buildingFinished()
        interactor.buildingFinished(isFunctional)
        supplier.calculateRemoteResources()
    }

    override fun canUpgrade(): Boolean = interactor.canUpgrade()

    override fun upgradeFinished(previous: Industry?) {
        super.upgradeFinished(previous)
        interactor.upgradeFinished(isFunctional)
        supplier.calculateRemoteResources()
    }

    override fun downgrade() {
        super.downgrade()
        interactor.downgrade()
        supplier.calculateRemoteResources()
    }

    override fun notifyDisrupted() = interactor.notifyDisrupted()

    override fun disruptionFinished() {
        super.disruptionFinished()
        isFunctional
        interactor.disruptionFinished()
        supplier.calculateRemoteResources()
    }

    override fun notifyBeingRemoved(mode: MarketAPI.MarketInteractionMode?, forUpgrade: Boolean) {
        super.notifyBeingRemoved(mode, forUpgrade)
        interactor.notifyBeingRemoved(forUpgrade)
    }

    private fun getRemoteMiningDesc(source: RemoteRezSource): String {
        return view.getRemoteMiningDesc(source)
    }

    private fun getRemoteMiningBonusDesc(cond: String, isRoids: Boolean): String {
        return view.getRemoteMiningBonusDesc(cond, isRoids)
    }

    override fun canImproveToIncreaseProduction(): Boolean = interactor.canImprove()

    override fun addNonAICoreInstalledItems(
        mode: Industry.IndustryTooltipMode?,
        tooltip: TooltipMakerAPI?,
        expanded: Boolean
    ): Boolean {
        supplier.calculateRemoteResources()
        return super.addNonAICoreInstalledItems(mode, tooltip, expanded)
    }
}