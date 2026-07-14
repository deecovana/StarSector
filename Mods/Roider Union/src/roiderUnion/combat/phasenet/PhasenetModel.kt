package roiderUnion.combat.phasenet

import com.fs.starfarer.api.combat.CombatEntityAPI
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.ExternalStrings

class PhasenetModel {
    var forceMult = 0f
    var targetVector = Vector2f()
    var hullName = ExternalStrings.DEBUG_NULL
    var statusSpeed = 0
    var infoState = PhasenetInfoState.NO_TARGET
    var target: CombatEntityAPI? = null
    var focusPoint: Vector2f = Vector2f()
}
