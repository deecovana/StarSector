package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Stats
import roiderUnion.helpers.Helper
import roiderUnion.helpers.TPSHelper
import roiderUnion.ids.ShipsAndWings
import roiderUnion.ids.hullmods.RoiderHullmods
import java.util.*

class GarruchaDraw : BaseHullMod() {
    companion object {
        const val LAUNCHED = "roider_garruchaLaunched"
        const val LANDING = "roider_garruchaLanding"
        const val DISPLACEMENT = 200f
        val NUM_DRONES = mapOf(
            Pair(HullSize.CAPITAL_SHIP, 4f),
            Pair(HullSize.CRUISER, 3f),
            Pair(HullSize.DESTROYER, 2f),
            Pair(HullSize.FRIGATE, 1f)
        )
        @JvmField
        val VALID_HULLS = mutableListOf(
            ShipsAndWings.DESPERADO
        )
        const val INCREASED_DAMAGE = 30f
        const val SMOD_INCREASED_DAMAGE = 20f
        const val SMOD_SPEED_MULT = 0.8f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        stats?.numFighterBays?.modifyFlat(id, NUM_DRONES[hullSize] ?: 0f)
        stats?.fighterWingRange?.modifyMult(id, 0f)

        //stats.getDynamic().getMod(Stats.FIGHTER_REARM_TIME_EXTRA_FLAT_MOD).modifyFlat(id, EXTRA_REARM_TIME);
        stats?.dynamic?.getMod(Stats.FIGHTER_REARM_TIME_EXTRA_FRACTION_OF_BASE_REFIT_TIME_MOD)
            ?.modifyFlat(id, 1f)
//        val variant = stats?.variant ?: return
//        val num = NUM_DRONES[variant.hullSize]?.toInt() ?: return
//        for (i in 0 until  num) {
//            if (variant.wings.size <= i) variant.wings?.add(ShipsAndWings.GARRUCHA_WING)
//            else variant.wings?.set(i, ShipsAndWings.GARRUCHA_WING)
//        }
    }

    override fun isApplicableToShip(ship: ShipAPI?): Boolean {
        return if (ship?.variant?.hasHullMod(RoiderHullmods.FIGHTER_CLAMPS) == true) false
            else VALID_HULLS.contains(ship?.hullSpec?.baseHullId)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String? {
        return if (index == 0) "1/2/3/4" // extern
            else if (index == 1) Helper.floatToPercentString(INCREASED_DAMAGE)
            else null
    }

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        if (ship == null) return
        val rand = Random(Helper.random.nextLong())
        ship.allWings?.forEach {
            it.wingMembers?.filter { m -> m.isLiftingOff && m.customData?.containsKey(LAUNCHED) == false }?.forEach {
                    m -> m.location.x = 0f; m.location.y = -1000000f
            }
            it.wingMembers?.filterNot { m -> m.isLiftingOff || m.customData?.containsKey(LAUNCHED) == true }?.forEach { m ->
                m.setCustomData(LAUNCHED, true)
                TPSHelper.phaseIn(m)
                Helper.soundPlayer?.playSound("phase_anchor_vanish", 1f, 1f, ship.location, ship.velocity) // extern, custom sound
                m.location.x = ship.location.x + (ship.collisionRadius + rand.nextFloat() * DISPLACEMENT) * if (rand.nextBoolean()) 1f else -1f
                m.location.y = ship.location.y + (ship.collisionRadius + rand.nextFloat() * DISPLACEMENT) * if (rand.nextBoolean()) 1f else -1f
            }
            it.returning?.map { r -> r.fighter }?.filterNot { m -> m.customData.containsKey(LANDING) }?.forEach { m ->
                m.setCustomData(LANDING, true)
                TPSHelper.phaseOut(m)
            }
            it.returning?.map { r -> r.fighter }?.filter { m -> m.customData.containsKey(LANDING) }?.forEach { m -> m.abortLanding() }
        }
    }

    override fun applyEffectsToFighterSpawnedByShip(fighter: ShipAPI?, ship: ShipAPI?, id: String?) {
        if (fighter?.mutableStats == null || id == null) return
        val stats = fighter.mutableStats
        val damage = if (isSMod(ship)) INCREASED_DAMAGE else INCREASED_DAMAGE + SMOD_INCREASED_DAMAGE
        stats.armorDamageTakenMult?.modifyPercent(id, damage)
        stats.hullDamageTakenMult?.modifyPercent(id, damage)
        if (isSMod(ship)) stats.maxSpeed?.modifyMult(id, SMOD_SPEED_MULT)
        fighter.setLightDHullOverlay()
    }

    override fun hasSModEffect(): Boolean = true
    override fun isSModEffectAPenalty(): Boolean = true

    override fun getSModDescriptionParam(index: Int, hullSize: HullSize?): String? {
        return when (index) {
            0 -> Helper.floatToPercentString(INCREASED_DAMAGE + SMOD_INCREASED_DAMAGE)
            1 -> Helper.multToPercentString(1f - SMOD_SPEED_MULT)
            else -> null
        }
    }
}