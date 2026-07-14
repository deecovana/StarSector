package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

class PlayerProspecting : EveryFrameScript {
    companion object {
        const val ROIDS_PER_DAY = 0.1f
        const val MAX_ROIDS = 5f
    }

    private val interval = IntervalUtil(0.1f, 0.1f)

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (!interval.intervalElapsed()) return
        if (RoidMiningHelper.points > MAX_ROIDS) return
        RoidMiningHelper.points += getRegen(days)
    }

    private fun getRegen(days: Float): Float {
        return ROIDS_PER_DAY * days
    }
}