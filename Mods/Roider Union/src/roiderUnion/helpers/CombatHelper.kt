package roiderUnion.helpers

import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.util.Misc

object CombatHelper {
    fun isTargetInRange(ship: CombatEntityAPI, t: CombatEntityAPI?, range: Float): Boolean {
        if (t == null) return false
        var dist: Float = Misc.getDistance(ship.location, t.location)
        dist -= (ship.collisionRadius + t.collisionRadius) * 2
        return dist <= range
    }
}