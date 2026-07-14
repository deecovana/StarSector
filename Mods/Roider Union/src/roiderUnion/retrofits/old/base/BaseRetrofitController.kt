package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.thoughtworks.xstream.XStream

open class BaseRetrofitController(
    protected val originalPlugin: InteractionDialogPlugin?,
    protected val manager: BaseRetrofitManagerV2,
    protected val memory: MutableMap<String, MemoryAPI>
) : InteractionDialogPlugin {
    protected enum class OptionId {
        PICK_TARGET, PICK_SHIPS, PRIORITIZE, CANCEL_SHIPS, LEAVE, CONFIRM, CANCEL
    }

    companion object {
        const val COLUMNS = 7
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(BaseRetrofitController::class.java, "originalPlugin", "op")
            x.aliasAttribute(BaseRetrofitController::class.java, "manager", "m")
            x.aliasAttribute(BaseRetrofitController::class.java, "memoryMap", "mm")
            x.aliasAttribute(BaseRetrofitController::class.java, "dialog", "d")
            x.aliasAttribute(BaseRetrofitController::class.java, "text", "t")
            x.aliasAttribute(BaseRetrofitController::class.java, "options", "o")
            x.aliasAttribute(BaseRetrofitController::class.java, "visual", "v")
            x.aliasAttribute(BaseRetrofitController::class.java, "retrofits", "r")
            x.aliasAttribute(BaseRetrofitController::class.java, "selectedRetrofit", "s")
        }
    }

    val model = BaseRetrofitModel()
    val interactor = BaseRetrofitInteractor(model, manager)
    open val view = BaseRetrofitPluginView(model)

    protected var dialog: InteractionDialogAPI? = null
    protected var text: TextPanelAPI? = null
    protected var options: OptionPanelAPI? = null
    protected var visual: VisualPanelAPI? = null

    override fun init(dialog: InteractionDialogAPI?) {
        if (dialog == null) return
        interactor.init(dialog)
        view.init(dialog)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        interactor.optionSelected(optionText, optionData)
        view.showOptions()
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
        interactor.optionMousedOver(optionText, optionData)
        view.optionMousedOver(optionText, optionData)
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI> = memory

    override fun advance(amount: Float) {}
    override fun backFromEngagement(battleResult: EngagementResultAPI?) {}
    override fun getContext(): Any? = null
}