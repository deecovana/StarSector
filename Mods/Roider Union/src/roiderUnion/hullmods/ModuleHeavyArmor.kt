package roiderUnion.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.ShipsAndWings
import java.util.EnumMap

class ModuleHeavyArmor : BaseHullMod() {

    companion object {
        val HULL_SIZE: MutableMap<String, HullSize> = HashMap()

        init {
            HULL_SIZE[ShipsAndWings.ROACH_ARMOR] = HullSize.FRIGATE
            HULL_SIZE[ShipsAndWings.JANE_ARMOR] = HullSize.DESTROYER
            HULL_SIZE[ShipsAndWings.ONAGER_ARMOR] = HullSize.DESTROYER
            HULL_SIZE[ShipsAndWings.AUROCHS_ARMOR] = HullSize.DESTROYER
            HULL_SIZE[ShipsAndWings.FIRESTORM_LEFT] = HullSize.DESTROYER
            HULL_SIZE[ShipsAndWings.FIRESTORM_RIGHT] = HullSize.DESTROYER
            HULL_SIZE[ShipsAndWings.GAMBIT_ARMOR] = HullSize.CRUISER
            HULL_SIZE[ShipsAndWings.RANCH_ARMOR] = HullSize.CRUISER
            HULL_SIZE[ShipsAndWings.RANCH_DAMPER_ARMOR] = HullSize.CRUISER
            HULL_SIZE[ShipsAndWings.WRECKER_ARMOR] = HullSize.CRUISER
            HULL_SIZE[ShipsAndWings.TELAMON_FRONT] = HullSize.CAPITAL_SHIP
            HULL_SIZE[ShipsAndWings.TELAMON_LEFT] = HullSize.CAPITAL_SHIP
            HULL_SIZE[ShipsAndWings.TELAMON_RIGHT] = HullSize.CAPITAL_SHIP
        }

        const val MANEUVER_PENALTY = 10f
        private val mag: MutableMap<HullSize, Float> = EnumMap(HullSize::class.java)

        init {
            mag[HullSize.FRIGATE] = 150f
            mag[HullSize.DESTROYER] = 300f
            mag[HullSize.CRUISER] = 400f
            mag[HullSize.CAPITAL_SHIP] = 500f
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (Helper.anyNull(hullSize, stats)) return

        var size: HullSize? = hullSize
        val hullId = stats!!.variant.hullSpec.hullId
        if (hullId != null) size = HULL_SIZE[hullId]
        stats.armorBonus.modifyFlat(id, mag[size] ?: mag[HullSize.FRIGATE]!!)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String {
        if (Helper.anyNull(hullSize)) return ExternalStrings.DEBUG_NULL
        if (index == 0) return mag[HullSize.FRIGATE]!!.toInt().toString()
        if (index == 1) return mag[HullSize.DESTROYER]!!.toInt().toString()
        if (index == 2) return mag[HullSize.CRUISER]!!.toInt().toString()
        if (index == 3) return mag[HullSize.CAPITAL_SHIP]!!.toInt().toString()
        return if (index == 4) Helper.floatToPercentString(MANEUVER_PENALTY)
        else ExternalStrings.DEBUG_NULL
    }

    override fun isApplicableToShip(ship: ShipAPI?): Boolean {
        val hullId = ship?.hullSpec?.hullId
        return if (hullId != null) HULL_SIZE.containsKey(hullId)
        else false
    }
}