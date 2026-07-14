package roiderUnion

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

class TaskTimer(
    private val task: Runnable,
    minDays: Float,
    maxDays: Float = minDays
) : EveryFrameScript {
    private var done = false
    private val timer = IntervalUtil(minDays, maxDays)

    override fun advance(amount: Float) {
        timer.advance(Misc.getDays(amount))
        if (timer.intervalElapsed()) {
            done = true
            task.run()
        }
    }

    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false
}