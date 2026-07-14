package roiderUnion.nomads.bases

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.Aliases

class NomadBaseIntelPlugin(base: SectorEntityToken) : FleetEventListener, BaseIntelPlugin() {
    companion object {
        val log = Global.getLogger(NomadBaseIntelPlugin::class.java)

        fun alias(x: XStream) {
            val jClass = NomadBaseIntelPlugin::class.java
            x.alias(Aliases.NBIC, jClass)
            x.aliasAttribute(jClass, "model", "rM")
            x.aliasAttribute(jClass, "view", "rV")
            x.aliasAttribute(jClass, "interactor", "rI")
            NomadBaseIntelModel.alias(x)
            NomadBaseIntelInteractor.alias(x)
            NomadBaseIntelView.alias(x)
        }
    }

    private val model: NomadBaseIntelModel = NomadBaseIntelModel()
    private val interactor: NomadBaseIntelInteractor = NomadBaseIntelInteractor(model, base)
    private val view: NomadBaseIntelView = NomadBaseIntelView(model)

    override fun isHidden(): Boolean {
        if (super.isHidden()) return true
        return interactor.isHidden(timestamp)
    }

    override fun advanceImpl(amount: Float) {
        interactor.advance(amount, this, this)
    }

    override fun notifyEnding() {
        interactor.notifyEnding(this)
    }

    fun makeKnown() {
        makeKnown(null)
    }

    fun makeKnown(text: TextPanelAPI?) {
        interactor.makeKnown(this, text)
    }

    override fun sendUpdateIfPlayerHasIntel(listInfoParam: Any?, onlyIfImportant: Boolean, sendIfHidden: Boolean) {
        super.sendUpdateIfPlayerHasIntel(listInfoParam, onlyIfImportant, sendIfHidden)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        interactor.baseFleetDespawned(this)
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}

    override fun getSortString(): String {
        return interactor.getSortString()
    }

    override fun getName(): String {
        interactor.determineBaseState(this)
        return view.getName()
    }

    override fun getFactionForUIColors(): FactionAPI = view.getFactionForUIColors()

    override fun getSmallDescriptionTitle(): String {
        interactor.determineBaseState(this)
        return view.getName()
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        interactor.prepareSmallDescription(this)
        view.createSmallDescription(info, width, height)
    }

    override fun getIcon(): String? {
        return view.getIcon()
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val result = super.getIntelTags(map)
        result += interactor.getIntelTags()
        return result
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        return interactor.getMapLocation()
    }

}