package roiderUnion.world

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.IntervalUtil

class CodexLinkRefresher : EveryFrameScript {
    private val interval = IntervalUtil(0.1f,0.2f)

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        interval.advance(amount)
        if (!interval.intervalElapsed()) return
        RoiderCodex.checkLinkedUnlocks()
    }
}