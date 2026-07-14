package roiderUnion.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

// Modified from SPP_LightningGunOnHitEffect
// Credit: DarkRevenant
class ArcOnHitEffect : OnHitEffectPlugin {
    override fun onHit(
        projectile: DamagingProjectileAPI?,
        target: CombatEntityAPI?, point: Vector2f?,
        shieldHit: Boolean, damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    ) {
        if (Helper.anyNull(projectile?.weapon, target, point, engine)) return

        if (target is ShipAPI) {
            var emp = projectile!!.weapon.damage.fluxComponent
            val hitShield = target.getShield() != null && target.getShield().isWithinArc(point)

            // Deal extra EMP (minus amount already dealt) to fighters at point of impact
            if (!hitShield && target.isFighter) engine!!.applyDamage(
                target,
                point,
                0f,
                DamageType.ENERGY,
                emp * 5f - emp,
                false,
                false,
                projectile.source
            )
            var pierceChance = target.hardFluxLevel - 0.4f
            pierceChance *= target.mutableStats.dynamic.getValue(Stats.SHIELD_PIERCED_MULT)
            val piercedShield = hitShield && Math.random().toFloat() < pierceChance
            //                piercedShield = true;
            val distance = MathUtils.getDistance(target.getLocation(), point)
            val range = projectile.weapon.range * 2f //
            val brightness = (255f * max(min((range - distance) / distance, 0f), 1f)).toInt()
            if (!hitShield && Math.random().toFloat() < 0.75f || piercedShield) {
                emp *= 0.2f
                if (target.isFighter) emp *= 5f
                //                        float dam = projectile.getWeapon().getDamage().getDamage() * 0.05f;
                engine!!.spawnEmpArcPierceShields(
                    projectile.source, projectile.location, projectile.damageTarget, projectile.damageTarget,
                    DamageType.ENERGY,
                    0f,  // damage
                    emp,  // emp
                    100000f,  // max range
                    ZapArcKeeper.ZAP_ARC_SOUND,
                    20f,
                    Color(100, 125, 200, brightness),
                    Color(240, 250, 255, brightness)
                )
            }
        }
    }
}