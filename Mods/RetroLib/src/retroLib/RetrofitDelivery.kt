package retroLib

import com.fs.starfarer.api.fleet.FleetMemberAPI
import kotlin.math.roundToInt

class RetrofitDelivery(
    val source: FleetMemberAPI,
    val data: RetrofitData
) {
    var timeRemaining = data.time
    val isPaused: Boolean
        get() = pauseReasons.isNotEmpty()
    val pauseReasons = mutableSetOf<String>()
    var ready = data.time <= 0

    val daysRemaining: Int
        get() = timeRemaining.roundToInt()

    fun advance(days: Float) {
        if (isPaused) return
        timeRemaining -= days
        if (timeRemaining <= 0) ready = true
    }

    fun pause(reason: String) {
        pauseReasons += reason
    }

    fun unpause(reason: String) {
        pauseReasons.remove(reason)
    }
}
