package roiderUnion.retrofits.argos

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import retroLib.impl.BaseRetrofitPluginInteractor
import retroLib.impl.BaseRetrofitPluginView
import retroLib.impl.RetrofitPlugin
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIds

class ArgosRetrofitController(private val entity: SectorEntityToken) {

    private val interactor: BaseRetrofitPluginInteractor
    private val view: BaseRetrofitPluginView

    init {
        val model = ArgosRetrofitPluginModel()
        val filter = ArgosFilter(null, ArgosAdjuster(null))
        val manager = ArgosRetrofitManager(entity.faction, filter)
        val service = ArgosRetrofitDeliveryService()
        interactor = ArgosRetrofitInteractor(
            model,
            entity as CampaignFleetAPI,
            manager,
            service,
            mutableMapOf(),
            null,
            this::showRetrofittingMessages
        )
        view = ArgosRetrofitView(
            model,
            this::updateInteractor
        )
    }

    fun openDialog() {
        val plugin = RetrofitPlugin(interactor, view)
        Helper.sector?.campaignUI?.showInteractionDialog(plugin, entity)
        Helper.soundPlayer?.setSuspendDefaultMusicPlayback(true)
        Helper.soundPlayer?.playCustomMusic(1, 2, RoiderIds.Music.RETROFIT_MUSIC, true)
    }

    private fun updateInteractor() {
        interactor.update()
    }

    private fun showRetrofittingMessages(cost: Double, stripped: Boolean) {
        view.showRetrofittingMessages(cost, stripped)
    }
}