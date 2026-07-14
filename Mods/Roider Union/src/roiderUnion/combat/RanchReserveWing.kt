package roiderUnion.combat

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import kotlin.math.max

class RanchReserveWing : BaseShipSystemScript() {
    companion object {
        const val RD_NO_EXTRA_CRAFT = "rd_no_extra_craft"
        const val EXTRA_FIGHTER_DURATION = 30f
    }

    override fun apply(stats: MutableShipStatsAPI, id: String?, state: ShipSystemStatsScript.State?, effectLevel: Float) {
        val ship = stats.entity as? ShipAPI ?: return
        if (effectLevel == 1f) {
            for (bay in ship.launchBaysCopy) {
                if (bay.wing == null) continue
                bay.makeCurrentIntervalFast()
                val spec = bay.wing.spec
                val addForWing = getAdditionalFor(spec)
                val maxTotal = spec.numFighters + addForWing
                val actualAdd = maxTotal - bay.wing.wingMembers.size
                if (actualAdd > 0) {
                    bay.fastReplacements = bay.fastReplacements + addForWing
                    bay.extraDeployments = actualAdd
                    bay.extraDeploymentLimit = maxTotal
                    bay.extraDuration = EXTRA_FIGHTER_DURATION
                }
            }
        }
    }

    private fun getAdditionalFor(spec: FighterWingSpecAPI): Int {
        if (spec.hasTag(RD_NO_EXTRA_CRAFT)) return 0
        val size = spec.numFighters
        return max(1, size / 2)
    }


    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {}


    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State?, effectLevel: Float): StatusData? {
        return null
    }


    override fun isUsable(system: ShipSystemAPI?, ship: ShipAPI?): Boolean {
        return true
    }
}