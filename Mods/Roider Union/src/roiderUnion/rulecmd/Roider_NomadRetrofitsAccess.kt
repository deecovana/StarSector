package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AddBarEvent
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.isPirateFaction
import retroLib.impl.*
import roiderUnion.helpers.ExternalStrings
import roiderUnion.retrofits.NomadRetrofitsAdjuster
import roiderUnion.retrofits.NomadRetrofitsDeliveryService
import roiderUnion.retrofits.NomadRetrofitsFilter
import roiderUnion.retrofits.NomadRetrofitsPluginView

class Roider_NomadRetrofitsAccess : BaseCommandPlugin() {
    companion object {
        const val OPTION_ID = "roider_nomadRetrofitsAccess"
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
        val command: String = params?.getOrNull(0)?.getString(memoryMap) ?: return false
        val market = dialog.interactionTarget?.market ?: return false
        if (market.faction == null) return false
        when (command) {
            OPTION_ADD -> addBarEvent(market)
            OPTION_ACCESS -> access(dialog, market, memoryMap)
        }

        return true
    }

    private fun addBarEvent(market: MarketAPI) {
        val option = ExternalStrings.NOMAD_RETROFIT_OPTION
        val blurb = ExternalStrings.NOMAD_RETROFIT_BLURB
        val data = AddBarEvent.BarEventData(OPTION_ID, option, blurb)
        data.optionColor = market.faction?.color ?: return
        val events: AddBarEvent.TempBarEvents = AddBarEvent.getTempEvents(market) ?: return
        events.events[OPTION_ID] = data
    }

    private fun access(dialog: InteractionDialogAPI, market: MarketAPI, memoryMap: MutableMap<String, MemoryAPI>?) {
        val ignoreRep = market.faction.id == Factions.INDEPENDENT || market.faction.isPirateFaction()
        val model = BaseRetrofitPluginModel()
        val filter = NomadRetrofitsFilter(market, NomadRetrofitsAdjuster(ignoreRep))
        val manager = BaseRetrofitManager(market, market.faction, filter) // potential NPE
        val service = NomadRetrofitsDeliveryService(market)
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
        view = NomadRetrofitsPluginView(
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