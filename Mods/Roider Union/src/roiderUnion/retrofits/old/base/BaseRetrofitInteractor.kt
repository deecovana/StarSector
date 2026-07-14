package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI

open class BaseRetrofitInteractor(override val model: RetrofitPluginModel, manager: BaseRetrofitManagerV2) : RetrofitPluginInteractor {
    protected var retrofits: List<RetrofitData> = manager.retrofits
    protected var selectedRetrofit: FleetMemberAPI? = null

    override fun init(dialog: InteractionDialogAPI) {

    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
        TODO("Not yet implemented")
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        TODO("Not yet implemented")
    }
}
