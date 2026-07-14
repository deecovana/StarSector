package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.Misc

class PlayerDiveScorer : EveryFrameScript {
    companion object {
        const val DIVE_DAYS = 10f
    }


    private var done = false

    private var time = DIVE_DAYS

    override fun isDone(): Boolean = done

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        // Count when player picks up dive roid
        // Give score when player leaves dive: dust to pluton
        // - no numbers
        // Tell roids to be non-exclusive if player leaves dive early
        time -= Misc.getDays(amount)
        if (time <= 0) done = true
    }
}