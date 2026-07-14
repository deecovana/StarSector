package roiderUnion.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import org.lwjgl.util.vector.Vector2f

class ArcCannonOnHitEffect : OnHitEffectPlugin {
    override fun onHit(
        projectile: DamagingProjectileAPI?, target: CombatEntityAPI?,
        point: Vector2f?, shieldHit: Boolean, damageResult: ApplyDamageResultAPI?, engine: CombatEngineAPI?,
    ) {
        if (projectile != null && !shieldHit && target is ShipAPI) {
            engine?.spawnEmpArc(
                projectile.source, point, target, target,
                DamageType.ENERGY,
                projectile.damageAmount,
                projectile.empAmount,
                Float.MAX_VALUE,
                ZapArcKeeper.ZAP_ARC_SOUND,
                ZapArcKeeper.ZAP_THICKNESS,
                ZapArcKeeper.ZAP_COLOR_FRINGE,
                ZapArcKeeper.ZAP_COLOR_CORE
            )
        }
    }
}