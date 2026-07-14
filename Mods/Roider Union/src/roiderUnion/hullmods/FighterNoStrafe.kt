package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import roiderUnion.helpers.Helper

/**
 * Copied banano of doom's idea
 */
class FighterNoStrafe : BaseHullMod() {
    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        if (Helper.anyNull(ship)) return

        ship!!.giveCommand(ShipCommand.ACCELERATE, null, 0)
        ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT)
        ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT)
        ship.blockCommandForOneFrame(ShipCommand.DECELERATE)
    }
}