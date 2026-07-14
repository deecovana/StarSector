package roiderUnion.combat.entropyAmp

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import roiderUnion.helpers.Helper

class EntropyAmpLRStats : BaseShipSystemScript() {
    class TargetData(var ship: ShipAPI, var target: ShipAPI?) {
        var targetEffectPlugin: EveryFrameCombatPlugin? = null
        var currDamMult = 0f
        var elapsedAfterInState = 0f
    }

    private val view: EntropyAmpLRView
    private val interactor: EntropyAmpLRInteractor

    init {
        val model = EntropyAmpLRModel()
        view = EntropyAmpLRView(model)
        interactor = EntropyAmpLRInteractor(model)
    }

    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String?,
        state: State?,
        effectLevel: Float
    ) {
        if (Helper.anyNull(stats, id, state)) return

        val ship = if (stats!!.entity is ShipAPI) stats.entity as ShipAPI
        else return

        interactor.applyEntropy(id!!, state!!, effectLevel, ship, this::buildEntropyPlugin)
        view.showFloatyText(ship)
        view.showEmitterVisuals(state, effectLevel, ship)
        view.showEntropyVisuals(effectLevel)
    }

    private fun buildEntropyPlugin(targetData: TargetData): EveryFrameCombatPlugin {
        return object : BaseEveryFrameCombatPlugin() {
            override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
                interactor.entropyAdvance(targetData)
                view.maintainStatusForPlayerShip()
            }
        }
    }

    override fun getStatusData(
        index: Int,
        state: State?,
        effectLevel: Float
    ): StatusData? {
        return view.getStatusData(index, effectLevel)
    }

    override fun getInfoText(system: ShipSystemAPI?, ship: ShipAPI?): String? {
        if (Helper.anyNull(system, ship)) return null
        interactor.confirmTargetInfo(ship!!)
        return view.getInfoText(system!!)
    }

    override fun isUsable(system: ShipSystemAPI?, ship: ShipAPI?): Boolean {
        return if (Helper.anyNull(ship)) false
        else interactor.isSystemUsable(ship!!)
    }
}