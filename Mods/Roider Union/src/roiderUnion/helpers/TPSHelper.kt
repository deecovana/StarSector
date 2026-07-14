package roiderUnion.helpers

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript
import com.fs.starfarer.api.util.FaderUtil
import com.fs.starfarer.api.util.Misc
import roiderUnion.hullmods.TemporalPhaseShell_Fighter
import java.awt.Color

/**
 * Temporal Phase Shell
 */
object TPSHelper {
    fun phaseIn(ship: ShipAPI) {
        ship.addListener(TpsPhaseIn(ship))
    }

    fun phaseOut(ship: ShipAPI) {
        ship.addListener(TpsPhaseOut(ship))
    }

    class TpsPhaseIn(private val ship: ShipAPI) : AdvanceableListener {
        private var diveFader = FaderUtil(0f, 1f)

        override fun advance(amount: Float) {
            var c = ship.phaseCloak?.specAPI?.effectColor2 ?: Misc.getTextColor()
            c = Misc.setAlpha(c, 255)
            c = Misc.interpolateColor(c, Color.white, 0.5f)
            diveFader.advance(amount)
            ship.phaseCloak?.forceState(ShipSystemAPI.SystemState.OUT, 0f)
            diveFader.fadeIn()
            diveFader.advance(amount)
            val b = diveFader.brightness
            ship.extraAlphaMult2 = b
            val r = ship.collisionRadius * 5f
            ship.setJitter(this, c, b, 20, r * (1f - b))
            if (diveFader.isFadedIn) {
                ship.removeListener(this)
            }
        }

    }

    class TpsPhaseOut(private val ship: ShipAPI) : AdvanceableListener {
        private var diveProgress = 0f
        private var diveFader = FaderUtil(1f, 1f)

        init {
            ship.mutableStats?.hullDamageTakenMult?.modifyMult(TemporalPhaseShell_Fighter.ID, 0f)
        }

        override fun advance(amount: Float) {
            var c = ship.phaseCloak?.specAPI?.effectColor2 ?: Misc.getTextColor()
            c = Misc.setAlpha(c, 255)
            c = Misc.interpolateColor(c, Color.white, 0.5f)
            if (diveProgress == 0f) {
                if (ship.fluxTracker?.showFloaty() == true) {
                    val timeMult = ship.mutableStats?.timeMult?.modifiedValue ?: 1f
                    Helper.combatEngine?.addFloatingTextAlways(
                        ship.location,
                        "Phase Shell collapse!", // extern
                        NeuralLinkScript.getFloatySize(ship),
                        c,
                        ship,
                        16f * timeMult,
                        3.2f / timeMult,
                        1f / timeMult,
                        0f,
                        0f,
                        1f
                    )
                }
            }
            diveFader.advance(amount)
            diveProgress += amount / TemporalPhaseShell_Fighter.DEAD_DIVE_DURATION
            ship.phaseCloak?.forceState(ShipSystemAPI.SystemState.IN, ship.extraAlphaMult.coerceIn(diveProgress.coerceAtMost(0.99f), 1f))
            if (diveProgress >= 1f) {
                if (diveFader.isIdle) {
                    Helper.soundPlayer?.playSound("phase_anchor_vanish", 1f, 1f, ship.location, ship.velocity) // extern, custom sound
                }
                diveFader.fadeOut()
                diveFader.advance(amount)
                val b = diveFader.brightness
                ship.extraAlphaMult2 = b
                val r = ship.collisionRadius * 5f
                ship.setJitter(this, c, b, 20, r * (1f - b))
                if (diveFader.isFadedOut) {
                    ship.location.set(0f, -1000000f)
                    Helper.combatEngine?.removeEntity(ship)
                }
            }
        }
    }
}