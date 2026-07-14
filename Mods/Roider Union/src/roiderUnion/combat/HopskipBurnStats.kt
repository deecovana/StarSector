package roiderUnion.combat

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import roiderUnion.helpers.Helper

class HopskipBurnStats : BaseShipSystemScript() {

    var ship: ShipAPI? = null

    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String?,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ) {
        if (Helper.anyNull(stats, id)) return
        if (ship == null) ship = Helper.combatEngine?.ships?.firstOrNull { it.mutableStats === stats } ?: return
        if (state == ShipSystemStatsScript.State.IDLE || state == ShipSystemStatsScript.State.COOLDOWN) return
        ship!!.giveCommand(ShipCommand.ACCELERATE, null, 0)
        ship!!.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT)
        ship!!.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT)
        ship!!.blockCommandForOneFrame(ShipCommand.DECELERATE)
        stats!!.maxSpeed.modifyMult(id, (1.5f * effectLevel).coerceAtLeast(1f))
        stats.turnAcceleration.modifyFlat(id, 30f * effectLevel)
        stats.turnAcceleration.modifyPercent(id, 200f * effectLevel)
        stats.maxTurnRate.modifyFlat(id, 15f)
        stats.maxTurnRate.modifyPercent(id, 100f)
        stats.acceleration.modifyPercent(id, 200f * effectLevel)
        stats.deceleration.modifyPercent(id, 200f * effectLevel)
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(stats, id)) return
        stats!!.maxSpeed.unmodify(id)
        stats.maxTurnRate.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
    }
}