package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.campaign.InteractionDialogAPI

interface RetrofitPluginInteractor {
    val model: RetrofitPluginModel
    fun init(dialog: InteractionDialogAPI)
    fun optionMousedOver(optionText: String?, optionData: Any?)
    fun optionSelected(optionText: String?, optionData: Any?)
}