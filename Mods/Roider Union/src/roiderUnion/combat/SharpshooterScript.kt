package roiderUnion.combat

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import org.magiclib.kotlin.getDistanceSq
import roiderUnion.helpers.Helper
import kotlin.math.pow

class SharpshooterScript : BaseEveryFrameCombatPlugin() {
    companion object {
        const val PROJ_ID = "roider_marksman_shot"
        const val AOE_DIST = 15f
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val sharpshooters = Helper.combatEngine?.projectiles?.filter { it.projectileSpecId == PROJ_ID } ?: return
        if (sharpshooters.isEmpty()) return
        for (shot in sharpshooters) {
            if (shot.isExpired) continue
            val target = Helper.combatEngine?.missiles
                ?.filter { it.owner != shot.owner }
                ?.firstOrNull { it.location.getDistanceSq(shot.location) <= AOE_DIST.pow(2) } ?: continue
            shot.location.set(target.location)
        }
    }
}