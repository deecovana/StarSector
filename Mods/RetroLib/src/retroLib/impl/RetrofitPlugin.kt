package retroLib.impl

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import retroLib.Helper
import retroLib.api.RetrofitPluginInteractor
import retroLib.api.RetrofitPluginView

class RetrofitPlugin(
    private val interactor: RetrofitPluginInteractor,
    private val view: RetrofitPluginView
) : InteractionDialogPlugin {
    override fun init(dialog: InteractionDialogAPI?) {
        if (Helper.anyNull(dialog)) return
        interactor.init(dialog!!)
        view.init()
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        interactor.optionSelected(optionText, optionData)
        view.showOptionResult()
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
        interactor.optionMousedOver(optionText, optionData)
        view.showMouseOverResult()
    }

    override fun advance(amount: Float) {
        interactor.advance(amount)
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {}

    override fun getContext(): Any? {
        return interactor.context
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI> {
        return interactor.memoryMap
    }
}