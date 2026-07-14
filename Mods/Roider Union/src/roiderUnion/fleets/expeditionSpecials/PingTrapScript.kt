package roiderUnion.fleets.expeditionSpecials

import com.fs.starfarer.api.EveryFrameScript
import com.thoughtworks.xstream.XStream
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.RoiderIds

/**
 * Author: SafariJohn
 */
class PingTrapScript : EveryFrameScript {
    companion object {
        const val SOURCE = "roider_pingTrap"
        const val UPTIME = 1f
        const val DURATION = 2f
        const val DOWNTIME = 1f
        const val PROFILE_PENALTY = 5000f
        fun alias(x: XStream) {
            val jClass = PingTrapScript::class.java
            x.alias(Aliases.PNGTRP, jClass)
            x.aliasAttribute(jClass, "effectLevel", "e")
            x.aliasAttribute(jClass, "duration", "r")
            x.aliasAttribute(jClass, "up", "u")
            x.aliasAttribute(jClass, "down", "d")
            x.aliasAttribute(jClass, "start", "s")
        }
    }

    private var effectLevel = 0f
    private var duration = DURATION
    private var up: Boolean
    private var down: Boolean
    private var start: Boolean

    init {
        start = true
        up = true
        down = false
    }

    override fun advance(amount: Float) {
        if (up) {
            if (start) {
                Helper.soundPlayer?.playSound(
                    RoiderIds.Sounds.SENSOR_BURST_ON_WORLD, 1f, 1f,
                    Helper.sector?.playerFleet?.location ?: Vector2f(), Vector2f()
                )
                start = false
            }
            effectLevel += amount / UPTIME
            if (effectLevel >= 1) {
                effectLevel = 1f
                up = false
            }
        } else if (down) {
            effectLevel -= amount / DOWNTIME
        } else {
            duration -= amount
            if (duration <= 0) down = true
        }
        val profile = Helper.sector?.playerFleet?.stats?.sensorProfileMod ?: return
        if (effectLevel > 0) profile.modifyFlat(SOURCE,PROFILE_PENALTY * effectLevel)
        else profile.unmodifyFlat(SOURCE)
    }

    override fun isDone(): Boolean = down && effectLevel <= 0
    override fun runWhilePaused(): Boolean = false
}