package roiderUnion.nomads.old.bases

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.EconomyAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import roiderUnion.nomads.NomadsData

class NomadBaseIntel(base: SectorEntityToken) : BaseIntelPlugin(), EveryFrameScript,
    FleetEventListener, EconomyAPI.EconomyUpdateListener, NomadBaseIntelAPI {
    private val interactor: NomadBaseInteractor
    private val view: NomadBaseView

    init {
        val model = NomadBaseModel(base)
        interactor = NomadBaseInteractor(model)
        view = NomadBaseView(model)
    }

    override fun isNomadBase(): Boolean {
        return interactor.isNomadBase()
    }

    override fun arrival(group: NomadsData) {
        interactor.arrival(group)
    }

    override fun departure() {
        interactor.departure()
    }

    override fun isHidden(): Boolean {
        return interactor.isHidden(super.isHidden())
    }

    override fun advanceImpl(amount: Float) {
//        interactor.advance(amount, playerVisibleTimestamp, baseEntity)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
    }

    override fun commodityUpdated(commodityId: String?) {
    }

    override fun economyUpdated() {
    }

    override fun isEconomyListenerExpired(): Boolean = false
}