package roiderUnion.combat.phasenet

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import roiderUnion.helpers.Helper

class PhasenetController : BaseShipSystemScript() {
    private val model: PhasenetModel = PhasenetModel()
    private val interactor: PhasenetInteractor = PhasenetInteractor(model)
    private val view: PhasenetView = PhasenetView(model)

    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String?,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ) {
        if (Helper.anyNull(stats, id, state)) return
        interactor.applyEffects(stats!!, id!!, state!!, effectLevel)
        view.showEffects(stats, state, effectLevel)
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {
        super.unapply(stats, id)
        if (Helper.anyNull(stats, id)) return
        interactor.unapply(stats!!, id!!)
        view.resetEffects()
    }

    override fun getInfoText(system: ShipSystemAPI?, ship: ShipAPI?): String? {
        if (Helper.anyNull(system, ship)) return null
        interactor.selectInfoText(system!!, ship!!)
        return view.getInfoText()
    }

    override fun getStatusData(
        index: Int,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ): ShipSystemStatsScript.StatusData? {
        interactor.setStatusData()
        return view.getStatusData(index)
    }

    override fun isUsable(system: ShipSystemAPI?, ship: ShipAPI?): Boolean {
        if (Helper.anyNull(ship)) return false
        return interactor.isUsable(ship!!)
    }
}