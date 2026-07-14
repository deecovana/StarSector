package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.VisualPanelAPI

class BaseRetrofitPluginView(override val model: RetrofitPluginModel) : RetrofitPluginView {
    private lateinit var dialog: InteractionDialogAPI
    private lateinit var text: TextPanelAPI
    private lateinit var options: OptionPanelAPI
    private lateinit var visual: VisualPanelAPI

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        text = dialog.textPanel
        options = dialog.optionPanel
        visual = dialog.visualPanel
//        updateText()
//        updateOptions()
//        updateVisual()
    }

    override fun showOptions() {

    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {

    }
}
