package scripts.campaign.cleanup

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import scripts.campaign.cleanup.Roider_ExpeditionLootCleaner

/**
 * Author: SafariJohn
 */
class Roider_ExpeditionLootCleaner(private var token: SectorEntityToken?, private val duration: Float) :
    EveryFrameScript {
    private var elapsed = 0f
    override fun isDone(): Boolean {
        if (token != null && token!!.isExpired) cleanLoot()
        return token == null
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (token!!.isExpired) {
            cleanLoot()
            return
        }
        if (amount <= 0) {
            return  // happens during game load
        }
        val days = Global.getSector().clock.convertToDays(amount)
        elapsed += days
        if (duration - elapsed <= 0) {
            cleanLoot()
        }
    }

    private fun cleanLoot() {
        if (token == null) return
        if (!token!!.hasTag(Tags.HAS_INTERACTION_DIALOG)) {
            for (e in token!!.starSystem.allEntities) {
                if (e.orbitFocus === token) {
                    Misc.fadeAndExpire(e)
                }
            }
        }
        Misc.fadeAndExpire(token)
        token = null
    }

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_ExpeditionLootCleaner::class.java, "token", "t")
            x.aliasAttribute(Roider_ExpeditionLootCleaner::class.java, "duration", "d")
            x.aliasAttribute(Roider_ExpeditionLootCleaner::class.java, "elapsed", "e")
        }
    }
}