package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import java.awt.Color

class HazardHazards : EveryFrameWeaponEffectPlugin {
    companion object {
        fun getExplosionSpec(damage: Float): DamagingExplosionSpec = DamagingExplosionSpec(
            0.1f,
            80f,
            40f,
            damage,
            damage,
            CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
            CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
            5f,
            1f,
            3f,
            150,
            Color(255,165,135),
            Color(255,125,80)
        )

        class DelayedExplosionSpawner(private val weapon: WeaponAPI, private var booms: Int) : BaseEveryFrameCombatPlugin() {
            private val interval = IntervalUtil(0.05f, 0.333f)
            private val spec = getExplosionSpec(weapon.damage?.damage ?: 1f)
            override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
                if (booms == 0 || weapon.ammo <= 0) {
                    Helper.combatEngine?.removePlugin(this)
                    return
                }

                interval.advance(amount)
                if (interval.intervalElapsed()) {
                    booms--
                    weapon.ammo--
                    val location = weapon.location.plus(Vector2f(Helper.random.nextFloat() * 10f, Helper.random.nextFloat() * 10f))
                    Helper.combatEngine?.spawnDamagingExplosion(spec, weapon.ship, location, false)
                }
            }
        }
    }

    private val interval = IntervalUtil(0.08f, 0.12f)
    private var disabled = false
    private var destroyed = false

    private fun engageSafeties(weapon: WeaponAPI) {
        if (destroyed) return
        if (weapon.isDisabled && !disabled) {
            disabled = true
            val maxFail = weapon.spec?.burstSize ?: 0
            val fail = Helper.random.nextInt(maxFail).coerceAtMost(weapon.ammo)
            if (fail == 0) return
            Helper.combatEngine?.addPlugin(DelayedExplosionSpawner(weapon, fail))
        }
        if (disabled && !weapon.isDisabled) disabled = false
    }

    private fun blowMagazines(weapon: WeaponAPI) {
        if (weapon.isPermanentlyDisabled && !destroyed) {
            destroyed = true
            val damage = (weapon.damage?.damage ?: 1f) * weapon.ammo.coerceAtMost(weapon.spec?.burstSize ?: 0)
            Helper.combatEngine?.applyDamage(weapon.ship, weapon.location, damage, DamageType.HIGH_EXPLOSIVE, 0f, true, false, weapon.ship, false)
            Helper.combatEngine?.spawnDamagingExplosion(getExplosionSpec(damage), weapon.ship, weapon.location, false)
        }
        if (destroyed && !weapon.isPermanentlyDisabled) destroyed = false
    }

    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?) {
        if (weapon == null) return
        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        engageSafeties(weapon)
        blowMagazines(weapon)
    }
}