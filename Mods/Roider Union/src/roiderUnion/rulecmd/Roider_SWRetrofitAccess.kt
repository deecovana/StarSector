package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AddBarEvent
import com.fs.starfarer.api.util.Misc
import retroLib.impl.*
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.RoiderIndustries
import roiderUnion.ids.MemoryKeys
import roiderUnion.retrofits.*

/**
 * Author: SafariJohn
 */
class Roider_SWRetrofitAccess : BaseCommandPlugin() {
    companion object {
        const val OPTION_ADD = "addBarEvent"
        const val OPTION_RETRO = "retrofit"
        const val OPTION_FUNC = "shipworksFunctional"

        const val STRAIGHT_ID = "roider_swRetrofitStraight"
    }

    private lateinit var interactor: BaseRetrofitPluginInteractor
    private lateinit var view: ShipworksPluginView

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (params.isNullOrEmpty()) return false
        val command = params[0].getString(memoryMap) ?: return false
        val market = dialog?.interactionTarget?.market ?: return false
        when (command) {
            OPTION_ADD -> addBarEvent(market)
            OPTION_FUNC -> return market.hasFunctionalIndustry(RoiderIndustries.SHIPWORKS)
            OPTION_RETRO -> retrofit(dialog, market, memoryMap)
        }
        return true
    }

    private fun addBarEvent(market: MarketAPI) {
        val optionId = STRAIGHT_ID
        val data = AddBarEvent.BarEventData(optionId, ExternalStrings.SW_OPTION, ExternalStrings.SW_BLURB)
        data.optionColor = Misc.getHighlightColor()
        AddBarEvent.getTempEvents(market)?.events?.put(optionId, data)
    }

    private fun retrofit(dialog: InteractionDialogAPI, market: MarketAPI, memoryMap: MutableMap<String, MemoryAPI>?) {
        val model = BaseRetrofitPluginModel()
        val filter = ShipworksFilter(market, ShipworksAdjuster(market))
        val manager = ShipworksRetrofitManager(market, filter)
        val service = Memory.get(
            MemoryKeys.SW_RETROFITTER,
            market,
            { it is ShipworksDeliveryService },
            { createDeliveryService(market) }
        ) as ShipworksDeliveryService
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
        view = ShipworksPluginView(
            model,
            this::updateInteractor
        )
        val plugin = RetrofitPlugin(interactor, view)
        dialog.plugin = plugin
        plugin.init(dialog)
    }

    private fun createDeliveryService(market: MarketAPI) : ShipworksDeliveryService {
        val result = ShipworksDeliveryService(market)
        Helper.sector?.addScript(result)
        return result
    }

    private fun updateInteractor() {
        interactor.update()
    }

    private fun showRetrofittingMessages(cost: Double, stripped: Boolean) {
        view.showRetrofittingMessages(cost, stripped)
    }
}