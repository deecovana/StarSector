package scripts

import com.fs.starfarer.api.Global

/**
 * Author: SafariJohn
 */
object Roider_Debug {
    var TECH_EXPEDITIONS = false

    init {
        TECH_EXPEDITIONS = getDebugBoolean("roider_techExpDebug")
    }

    private fun getDebugBoolean(id: String): Boolean {
        try {
            return Global.getSettings().getBoolean(id)
        } catch (ex: Exception) {
        }
        return false
    }
}