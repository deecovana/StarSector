package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import roiderUnion.helpers.Helper
import kotlin.math.min

/**
 * Author: SafariJohn
 */
class RotaryHammerReloadScript : EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    companion object {
        const val MAX_AMMO = 3
        const val RELOAD_SIZE = 2
        const val RELOAD_TIME = 4.45f // seconds
    }

    private val animation = RotaryHammerAnimation()
    private val ammoLights = RotaryHammerAmmoLights()
    private val reload = IntervalUtil(RELOAD_TIME, RELOAD_TIME)
    private var drumAmmo = MAX_AMMO
    private var isDrumEmpty = false

    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?) {
        if (Helper.anyNull(engine, weapon)) return

        animation.advance(amount, engine!!, weapon!!)
        ammoLights.advance(amount, engine, weapon, drumAmmo)

        if (engine.isPaused) return

        val ammoInconsistency = drumAmmo > weapon.ammo
        if (ammoInconsistency) drumAmmo = weapon.ammo

        reload.advance(amount)
        if (reload.intervalElapsed()) {
            val wasEmpty = drumAmmo == 0
            drumAmmo += RELOAD_SIZE
            drumAmmo = min(drumAmmo, MAX_AMMO)
            drumAmmo = min(drumAmmo, weapon.ammo)
            isDrumEmpty = drumAmmo == 0
            if (wasEmpty && !isDrumEmpty) {
                animation.onFire(null, weapon, engine)
            }
        }

        if (drumAmmo == 0 && weapon.ammo > 0) {
            if (isDrumEmpty) {
                isDrumEmpty = false
                drumAmmo = min(RELOAD_SIZE, weapon.ammo)
                animation.onFire(null, weapon, engine)
            } else {
                weapon.setRemainingCooldownTo(weapon.cooldown * (reload.elapsed / getReloadTime(weapon)))
            }
        }
    }

    private fun getReloadTime(weapon: WeaponAPI): Float {
        return RELOAD_TIME * getMissileRoFMult(weapon.ship)
    }

    private fun getMissileRoFMult(ship: ShipAPI?) = ship?.mutableStats?.missileRoFMult?.modifiedValue ?: 1f

    private fun isReloadLessThanCooldown(weapon: WeaponAPI?): Boolean {
        return if (weapon != null) getReloadTime(weapon) - reload.elapsed < weapon.cooldown else false
    }

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        drumAmmo--
        if (drumAmmo > 0) {
            animation.onFire(projectile, weapon, engine)
            reload.elapsed = 0f
        } else if (drumAmmo == 0 && isReloadLessThanCooldown(weapon)) {
            reload.elapsed = weapon!!.cooldown
        }
    }
}