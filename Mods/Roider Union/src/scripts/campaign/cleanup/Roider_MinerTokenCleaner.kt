package scripts.campaign.cleanup

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import scripts.campaign.cleanup.Roider_MinerTokenCleaner

/**
 * Author: SafariJohn
 */
class Roider_MinerTokenCleaner(private var route: RouteData?, private var token: SectorEntityToken?) : EveryFrameScript {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_MinerTokenCleaner::class.java, "route", "r")
            x.aliasAttribute(Roider_MinerTokenCleaner::class.java, "token", "t")
        }
    }

    override fun isDone(): Boolean {
        if (token != null && token!!.isExpired) cleanToken()
        return token == null
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (!RouteManager.getInstance().getRoutesForSource(route!!.source).contains(route)) {
            cleanToken()
        }
    }

    private fun cleanToken() {
        if (token == null) return
        route = null
        Misc.fadeAndExpire(token)
        token = null
    }
}