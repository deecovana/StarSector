package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import retroLib.impl.BaseRetrofitPluginModel
import retroLib.impl.BaseRetrofitPluginView
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIds.Music

class ShipworksPluginView(
    model: BaseRetrofitPluginModel,
    updateInteractor: () -> Unit
) : BaseRetrofitPluginView(model, updateInteractor) {
    override fun init() {
        super.init()
        Helper.soundPlayer?.setSuspendDefaultMusicPlayback(true)
        Helper.soundPlayer?.playCustomMusic(1, 1, Music.RETROFIT_MUSIC, true)
    }

    override val notAllowedRetrofitsTitle: String
        get() = ExternalStrings.SW_BP_REQ

    override fun getNotAllowedRetrofitText(spec: ShipHullSpecAPI): String {
        return ExternalStrings.SW_BP_UNKNOWN
    }

    override fun setNotAllowedTooltip(options: OptionPanelAPI, option: String) {
        options.setTooltip(option, ExternalStrings.SW_BP_UNKNOWN_TT)
    }
}