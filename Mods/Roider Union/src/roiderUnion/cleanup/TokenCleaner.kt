package roiderUnion.cleanup

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.Misc

class TokenCleaner(private val token: SectorEntityToken, private val test: () -> Boolean) : EveryFrameScript {
    var done = false

    override fun isDone(): Boolean = done

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (!done && test()) {
            done = true
            Misc.fadeAndExpire(token)
        }
    }
}