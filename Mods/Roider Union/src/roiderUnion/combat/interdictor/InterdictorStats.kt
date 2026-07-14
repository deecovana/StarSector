package roiderUnion.combat.interdictor

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import roiderUnion.helpers.Helper
import java.awt.Color

/**
 * Knocks out all engines instead of less than half.
 */
class InterdictorStats : BaseShipSystemScript() {
    companion object {
        val EFFECT_COLOR = Color(100, 165, 255, 75)
    }

    val view: InterdictorView
    val interactor: InterdictorInteractor

    init {
        val model = InterdictorModel()
        view = InterdictorView(model)
        interactor = InterdictorInteractor(model)
    }

    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ) {
        if (Helper.anyNull(stats, state)) return

        val ship = if (stats!!.entity is ShipAPI) stats.entity as ShipAPI else return
        interactor.applyInterdiction(ship, state!!, effectLevel)
        view.showFloaty(ship)
        view.showJitter(ship, state, effectLevel)
    }

    override fun getInfoText(system: ShipSystemAPI?, ship: ShipAPI?): String? {
        if (Helper.anyNull(system, ship)) return null
        interactor.confirmTargetInfo(system!!, ship!!)
        return view.getInfoText()
    }

    override fun isUsable(system: ShipSystemAPI?, ship: ShipAPI?): Boolean {
        if (Helper.anyNull(system, ship)) return false
        return interactor.isUsable(system!!, ship!!)
    }
}