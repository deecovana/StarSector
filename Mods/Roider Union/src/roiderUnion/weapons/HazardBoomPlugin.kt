package roiderUnion.weapons

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import roiderUnion.helpers.Helper

class HazardBoomPlugin(private val missile: MissileAPI) : BaseEveryFrameCombatPlugin() {
    private val interval = IntervalUtil(0.08f, 0.12f)

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        interval.advance(amount)
        if (interval.intervalElapsed()) {
            if (missile.hullLevel <= 0 && !missile.didDamage()) {
                missile.explode()
                Helper.combatEngine?.removePlugin(this)
            } else if (isMissileDone()) {
                Helper.combatEngine?.removePlugin(this)
            }
        }
    }

    private fun isMissileDone(): Boolean = missile.isExpired || missile.didDamage()
            || Helper.combatEngine?.isEntityInPlay(missile) == false
}