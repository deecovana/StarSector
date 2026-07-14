package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Pair
import roiderUnion.helpers.Helper

/**
 * Author: SafariJohn
 */
class RotaryHammerAnimation : EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    companion object {
        private class Animation(vararg val frames: Pair<Int, Float>) {
            val length: Float
            init {
                var l = 0f
                frames.forEach { l += it.two }
                length = l
            }
        }

        const val CLICK_SOUND = "roider_rhl_click"
        const val CLACK_SOUND = "roider_rhl_clack"
        const val LOUD_CLICK_SOUND = "roider_rhl_clickLoud"
        const val LOUD_CLACK_SOUND = "roider_rhl_clackLoud"
        const val PAD = 1.1f
        private const val SLOW_SPEED = 0.8f
        private const val FAST_SPEED = 1.5f
        private val FRAMES = mutableListOf<Animation>()

        private val ROLL = Animation(
            Pair(0, 0.02f),
            Pair(1, 0.07f),
            Pair(2, 0.06f),
            Pair(3, 0.05f),
            Pair(4, 0.04f),
            Pair(5, 0.03f),
            Pair(6, 0.03f),
            Pair(0, 0.02f)
        )
        private val ROLL_SLOW = Animation(
            Pair(0, 0.02f),
            Pair(1, 0.08f),
            Pair(2, 0.07f),
            Pair(3, 0.06f),
            Pair(4, 0.05f),
            Pair(5, 0.04f),
            Pair(6, 0.04f),
            Pair(0, 0.03f)
        )
        private val ROLLBACK = Animation(
            Pair(0, 0.02f),
            Pair(1, 0.07f),
            Pair(2, 0.06f),
            Pair(3, 0.05f),
            Pair(4, 0.04f),
            Pair(5, 0.03f),
            Pair(6, 0.03f),
            Pair(0, 0.04f),
            Pair(1, 0.05f),
            Pair(0, 0.02f)
        )
        private val ROLLBACK_LARGE = Animation(
            Pair(0, 0.02f),
            Pair(1, 0.07f),
            Pair(2, 0.06f),
            Pair(3, 0.05f),
            Pair(4, 0.04f),
            Pair(5, 0.03f),
            Pair(6, 0.03f),
            Pair(0, 0.04f),
            Pair(1, 0.05f),
            Pair(2, 0.07f),
            Pair(1, 0.08f),
            Pair(0, 0.03f)
        )

        init {
            reloadAnimationFrames()
        }

        private fun reloadAnimationFrames() {
            FRAMES.clear()
            FRAMES.add(ROLL)
            FRAMES.add(ROLL_SLOW)
            FRAMES.add(ROLLBACK)
            FRAMES.add(ROLLBACK_LARGE)
        }

        private val randomSpeedMult: Float
            get() = FAST_SPEED + Helper.random.nextFloat() * (SLOW_SPEED - FAST_SPEED)
    }

    private var fired = false
    private var animating = false
    private var repeat = false
    private var outOfAmmo = false
    private var regenCycle = false
    private var frame = 0
    private var playSpeed = 1f
    private val tracker: IntervalUtil = IntervalUtil(0f, 0f)
    private var animation = Animation()

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        fired = true
        frame = 0
        weapon?.animation?.frame = 0
    }

    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?) {
        if (Helper.anyNull(engine, weapon?.animation)) return
        if (engine!!.isPaused) return
        if (weapon!!.ammo == 0) {
            outOfAmmo = true
            return
        }
        if (outOfAmmo && weapon.ammo > 0) regenStart()
        if (!animating && !fired) {
            stop(weapon)
            return
        }
        if (weapon.isDisabled) return
        if (fired) {
            start()
        }
        val ship = weapon.ship ?: return
        tracker.advance(amount / getCurrentPlaySpeed(weapon, ship))
        if (!tracker.intervalElapsed()) return
        if (animation.frames.isEmpty()) return
        nextFrame(weapon, ship)
        setFrameDelay()
    }

    private fun start() {
        fired = false
        animating = true
        repeat = false
        animation = FRAMES[Helper.random.nextInt(FRAMES.size)]
        playSpeed = randomSpeedMult
    }

    private fun regenStart() {
        outOfAmmo = false
        regenCycle = true
        start()
    }

    private fun stop(weapon: WeaponAPI) {
        reloadAnimationFrames()
        regenCycle = false
        weapon.animation?.pause()
        frame = 0
        weapon.animation?.frame = 0
    }

    private fun getCurrentPlaySpeed(weapon: WeaponAPI, ship: ShipAPI): Float {
        var result = playSpeed
        val cooldown =  weapon.cooldown * getMissileRoFMult(ship)
        if (animation.length * result >= cooldown) {
            result *= animation.length * PAD / cooldown
        }
        if (weapon.cooldownRemaining <= animation.length * result) {
            result *= weapon.cooldownRemaining / (animation.length * PAD * result)
        }
        return result
    }

    private fun getMissileRoFMult(ship: ShipAPI) = ship.mutableStats?.missileRoFMult?.modifiedValue ?: 1f

    private fun nextFrame(weapon: WeaponAPI, ship: ShipAPI) {
        frame++
        if (frame >= animation.frames.size) {
            playClack(weapon, ship)
            frame = 0
            if (repeat) {
                repeat = false
            } else {
                animating = false
            }
        } else {
            playClick(weapon, ship)
        }
        weapon.animation?.frame = animation.frames[frame].one
    }

    private fun setFrameDelay() {
        if (animating && !repeat && frame == 0) {
            tracker.setInterval(0.3f, 0.6f)
        } else {
            tracker.setInterval(
                animation.frames[frame].two,
                animation.frames[frame].two
            )
        }
    }

    private fun playClick(weapon: WeaponAPI, ship: ShipAPI) {
        if (regenCycle) {
            Helper.soundPlayer?.playSound(LOUD_CLICK_SOUND, 1f, 1f, weapon.location, ship.velocity)
        } else {
            Helper.soundPlayer?.playSound(CLICK_SOUND, 1f, 1f, weapon.location, ship.velocity)
        }
    }

    private fun playClack(weapon: WeaponAPI, ship: ShipAPI) {
        if (regenCycle) {
            Helper.soundPlayer?.playSound(LOUD_CLACK_SOUND, 1f, 1f, weapon.location, ship.velocity)
        } else {
            Helper.soundPlayer?.playSound(CLACK_SOUND, 1f, 1f, weapon.location, ship.velocity)
        }
    }
}