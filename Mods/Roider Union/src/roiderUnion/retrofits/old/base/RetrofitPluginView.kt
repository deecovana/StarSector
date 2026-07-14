package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.campaign.InteractionDialogAPI

interface RetrofitPluginView {
    val model: RetrofitPluginModel
    fun init(dialog: InteractionDialogAPI)
    fun showOptions()
    fun optionMousedOver(optionText: String?, optionData: Any?)
}