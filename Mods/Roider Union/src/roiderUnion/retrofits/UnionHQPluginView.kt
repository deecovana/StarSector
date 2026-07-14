package roiderUnion.retrofits

import retroLib.impl.BaseRetrofitPluginModel
import retroLib.impl.BaseRetrofitPluginView
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIds.Music

class UnionHQPluginView(
    model: BaseRetrofitPluginModel,
    updateInteractor: () -> Unit
) : BaseRetrofitPluginView(model, updateInteractor) {

    override fun init() {
        super.init()
        Helper.soundPlayer?.setSuspendDefaultMusicPlayback(true)
        Helper.soundPlayer?.playCustomMusic(1, 1, Music.RETROFIT_MUSIC, true)
    }
}