package retroLib.impl.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AddBarEvent
import com.fs.starfarer.api.util.Misc
import retroLib.Helper
import retroLib.Settings
import retroLib.impl.*

class RetroLib_DefaultAccess : BaseCommandPlugin() {
    companion object {
        const val OPTION_ADD = "addBarEvent"
        const val OPTION_ACCESS = "access"
    }

    private lateinit var interactor: BaseRetrofitPluginInteractor
    private lateinit var view: BaseRetrofitPluginView

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false
        if (Helper.settings?.isDevMode == false || !Settings.DEV_MODE_RETROFITTER) return false
        val command: String = params?.getOrNull(0)?.getString(memoryMap) ?: return false
        val market = dialog.interactionTarget?.market ?: return false
        when (command) {
            OPTION_ADD -> addBarEvent(market)
            OPTION_ACCESS -> access(dialog, market, memoryMap)
        }


        return true
    }

    private fun addBarEvent(market: MarketAPI) {
        val optionId = "RetroLib_barAccess"
        val option = "Access retrofitting" // extern
        val blurb = "RetroLib access blurb"
        val data = AddBarEvent.BarEventData(optionId, option, blurb)
        data.optionColor = market.faction.color
        val events: AddBarEvent.TempBarEvents = AddBarEvent.getTempEvents(market) ?: return
        events.events[optionId] = data
    }

    private fun access(dialog: InteractionDialogAPI, market: MarketAPI, memoryMap: MutableMap<String, MemoryAPI>?) {
        val model = BaseRetrofitPluginModel()
        val filter = BaseRetrofitFilter(market, BaseRetrofitAdjuster(market))
        val manager = BaseRetrofitManager(market, market.faction, filter)
        val service = BaseRetrofitDeliveryService(market)
        val originalPlugin = dialog.plugin
        interactor = BaseRetrofitPluginInteractor(
            model,
            market.faction,
            manager,
            service,
            memoryMap ?: mutableMapOf(),
            originalPlugin,
            this::showRetrofittingMessages
        )
        view = BaseRetrofitPluginView(
            model,
            this::updateInteractor
        )
        val plugin = RetrofitPlugin(interactor, view)
        dialog.plugin = plugin
        plugin.init(dialog)
    }

    private fun updateInteractor() {
        interactor.update()
    }

    private fun showRetrofittingMessages(cost: Double, stripped: Boolean) {
        view.showRetrofittingMessages(cost, stripped)
    }
}