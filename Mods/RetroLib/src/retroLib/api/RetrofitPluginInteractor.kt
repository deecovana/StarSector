package retroLib.api

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI

interface RetrofitPluginInteractor {
    val context: Any?
    val memoryMap: MutableMap<String, MemoryAPI>
    fun init(dialog: InteractionDialogAPI)
    fun optionSelected(optionText: String?, optionData: Any?)
    fun optionMousedOver(optionText: String?, optionData: Any?)
    fun advance(amount: Float)
}