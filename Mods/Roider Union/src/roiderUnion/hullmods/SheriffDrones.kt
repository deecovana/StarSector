package roiderUnion.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.FighterWingAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Stats
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Author: SafariJohn
 * Unused
 */
class SheriffDrones : BaseHullMod() {

    companion object {
        const val EW_PENALTY_MULT = 0.5f
        const val ENGAGEMENT_REDUCTION_MULT = 0f

        //    public static final Color DRONE_ENGINE_COLOR = new Color(255,255,0,0);
        val DRONE_ENGINE_COLOR = Color(170, 220, 222, 255)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String {
        if (index == 0) return ((1f - ENGAGEMENT_REDUCTION_MULT) * 100f).roundToInt().toString() + "%"
        return if (index == 1) ((1f - EW_PENALTY_MULT) * 100f).roundToInt().toString() + "%"
        else ExternalStrings.DEBUG_NULL
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        stats?.fighterWingRange?.modifyMult(id, ENGAGEMENT_REDUCTION_MULT, "Drone Network")
    }

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        if (Global.getCombatEngine() == null) return
        if (Global.getCombatEngine().ships == null) return
        var numDronesAlive = 0f
        var wing: FighterWingAPI? = null
        for (s in Global.getCombatEngine().ships) {
            if (!s.isFighter) continue
            if (!s.isAlive) continue
            val w: FighterWingAPI = s.wing ?: continue
            if (w.sourceShip === ship) {
                numDronesAlive++
                wing = w
                s.engineController.fadeToOtherColor(this, DRONE_ENGINE_COLOR, null, 1f, 1.5f)
                //                s.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
            }
        }
        if (wing != null) {
            val mult: Float = numDronesAlive / wing.spec.numFighters.toFloat()
            ship?.mutableStats?.dynamic?.getStat(Stats.ELECTRONIC_WARFARE_PENALTY_MULT)?.modifyMult(
                    RoiderHullmods.SHERIFF_DRONES + ship.id,
                    1f - EW_PENALTY_MULT * mult
                )
        }
    }

    override fun applyEffectsToFighterSpawnedByShip(fighter: ShipAPI?, ship: ShipAPI?, id: String?) {
        if (Helper.anyNull(fighter, id)) return

        val stats = fighter!!.mutableStats
        stats.engineDamageTakenMult.modifyMult(id, 0f)
        stats.autofireAimAccuracy.modifyFlat(id, 1f)
        stats.dynamic.getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f)
    }
}