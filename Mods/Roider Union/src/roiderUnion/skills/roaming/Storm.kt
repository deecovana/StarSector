package roiderUnion.skills.roaming

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.SkillsHelper
import roiderUnion.ids.Icons
import roiderUnion.ids.hullmods.RoiderHullmods
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class Storm : SCBaseSkillPlugin() {
    companion object {
        const val EMERGENCY_BURN_BONUS = 2f
//        const val ZERO_FLUX_SPEED_BOOST = 10f
        const val BOOST_DURATION = 2f
        const val BOOST_WAS_ACTIVE_KEY = "\$roider_stormEngineWasBoosted"
//        const val OP_REDUCTION_SMALL = 1
//        const val OP_REDUCTION_MEDIUM = 1
//        const val OP_REDUCTION_LARGE = 2
        const val ACTIVE_VENT_BUFF = 10f
        const val PASSIVE_VENT_BUFF = 20f
        const val SMALL_RANGE_CUTOFF = 600f
        const val MEDIUM_RANGE_CUTOFF = 700f
        const val LARGE_RANGE_CUTOFF = 800f
        const val FULL_EFFECT_RANGE_MULT = 0.5f

        private const val RANGE_TOKEN = "[RANGES]"
        private const val BOOST_TOKEN = "[BOOST]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_ALL_SHIPS

    override fun addTooltip(data: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.STORM_EBURN_BOOST.replace(BOOST_TOKEN, EMERGENCY_BURN_BONUS.toInt().toString()))
        tooltip?.addSpacer(Helper.SMALL_PAD)
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.STORM_VENT_BOOST.replace(BOOST_TOKEN, ACTIVE_VENT_BUFF.toInt().toString()))
        tooltip?.addSpacer(Helper.SMALL_PAD)
        val rangeCutoffs = SMALL_RANGE_CUTOFF.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                MEDIUM_RANGE_CUTOFF.toInt() + ExternalStrings.LIST_DIVIDER +
                LARGE_RANGE_CUTOFF.toInt()
//        val opReductions = OP_REDUCTION_SMALL.toString() + ExternalStrings.LIST_DIVIDER +
//                OP_REDUCTION_MEDIUM + ExternalStrings.LIST_DIVIDER +
//                OP_REDUCTION_LARGE
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.STORM_DISS_BOOST.replace(RANGE_TOKEN, rangeCutoffs).replace(BOOST_TOKEN, PASSIVE_VENT_BUFF.toInt().toString()))
        tooltip?.addSpacer(Helper.SMALL_PAD)
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.STORM_SPEED_RETENTION.replace(BOOST_TOKEN, BOOST_DURATION.toInt().toString()))
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData?,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI?,
        hullSize: ShipAPI.HullSize?,
        id: String?
    ) {
        stats?.ventRateMult?.modifyPercent(id, ACTIVE_VENT_BUFF)
//        stats?.zeroFluxSpeedBoost?.modifyFlat(id, ZERO_FLUX_SPEED_BOOST)
    }

    override fun advanceInCombat(data: SCData?, ship: ShipAPI?, amount: Float?) {
        if (ship == null || amount == null) return
        modifySpeed(ship, amount)
        modifyVentRate(ship)
    }

    private fun modifySpeed(ship: ShipAPI, amount: Float) {
        if (ship.isEngineBoostActive) {
            ship.mutableStats?.maxSpeed?.unmodifyFlat(id)
            ship.setCustomData(BOOST_WAS_ACTIVE_KEY, 1f)
        } else if (ship.customData.containsKey(BOOST_WAS_ACTIVE_KEY)) {
            val effectLevel = getBoostRetentionEffectLevel(ship)
            if (effectLevel <= 0f) {
                ship.mutableStats?.maxSpeed?.unmodifyFlat(id)
                ship.removeCustomData(BOOST_WAS_ACTIVE_KEY)
                return
            }
            val speed = effectLevel * (ship.mutableStats?.zeroFluxSpeedBoost?.modified ?: 0f)
            ship.setCustomData(BOOST_WAS_ACTIVE_KEY, ship.customData[BOOST_WAS_ACTIVE_KEY] as Float - amount / BOOST_DURATION)
            ship.mutableStats?.maxSpeed?.modifyFlat(id, speed)
            if (ship == Helper.combatEngine?.playerShip) {
                Helper.combatEngine?.maintainStatusForPlayerShip(
                    id,
                    Helper.settings?.getSpriteName(Icons.CAT_UI, Icons.ENGINE_BOOST) ?: Icons.ENGINE_BOOST,
                    ExternalStrings.STORM_SKILL_MOD_DESC,
                    ExternalStrings.STORM_SPEED_RETENTION_COMBAT.replace(BOOST_TOKEN, speed.toInt().toString()),
                    false
                )
            }
        } else {
            ship.mutableStats?.maxSpeed?.unmodifyFlat(id)
        }
    }

    private fun getBoostRetentionEffectLevel(ship: ShipAPI): Float {
        return ship.customData[BOOST_WAS_ACTIVE_KEY] as? Float ?: 0f
    }

    private fun modifyVentRate(ship: ShipAPI) {
        val range = ship.mutableStats?.ballisticWeaponRangeBonus?.computeEffective(getVentRangeCutoff(ship)) ?: getVentRangeCutoff(ship)
        val nearestEnemy = Misc.findClosestShipEnemyOf(ship, ship.location, ShipAPI.HullSize.FRIGATE, range, true)
        if (nearestEnemy != null) {
            val dist = Misc.getDistance(nearestEnemy.location, ship.location) - nearestEnemy.collisionRadius - ship.collisionRadius
            val fullEffectRange = range * FULL_EFFECT_RANGE_MULT
            val effectLevel = if (dist <= fullEffectRange) 1f else 1f - ((dist - fullEffectRange) / (range - fullEffectRange))
            val buff = PASSIVE_VENT_BUFF * effectLevel
            ship.mutableStats?.fluxDissipation?.modifyPercent(id, buff)
            if (ship == Helper.combatEngine?.playerShip) {
                Helper.combatEngine?.maintainStatusForPlayerShip(
                    id,
                    Helper.settings?.getSpriteName(Icons.CAT_UI, Icons.NEBULA),
                    ExternalStrings.STORM_SKILL_MOD_DESC,
                    ExternalStrings.STORM_DISS_BOOST_COMBAT.replace(BOOST_TOKEN, buff.toInt().toString()),
                    false
                )
            }
        } else {
            ship.mutableStats?.fluxDissipation?.unmodify(id)
        }
    }

    private fun getVentRangeCutoff(ship: ShipAPI): Float {
        val weapons = ship.hullSpec?.allWeaponSlotsCopy?.filterNot { it.weaponType == WeaponAPI.WeaponType.MISSILE } ?: emptyList()
        return if (weapons.any { it.slotSize == WeaponAPI.WeaponSize.LARGE }) {
            LARGE_RANGE_CUTOFF
        } else if (weapons.any { it.slotSize == WeaponAPI.WeaponSize.MEDIUM }) {
            MEDIUM_RANGE_CUTOFF
        } else {
            SMALL_RANGE_CUTOFF
        }
    }

    override fun advance(data: SCData?, amount: Float?) {
        if (data?.fleet?.stats == null) return
        modifyEBurn(data.fleet.stats)
//        data.fleet.fleetData?.membersListCopy?.filterNot { it.variant?.hasHullMod(RoiderHullmods.STORM_SKILL_MOD) == true }
//            ?.forEach { it.variant?.addMod(RoiderHullmods.STORM_SKILL_MOD) }
    }

    override fun onActivation(data: SCData?) {
        if (data?.fleet?.stats == null) return
        modifyEBurn(data.fleet.stats)
//        data.fleet.fleetData?.membersListCopy?.forEach { it.variant?.addMod(RoiderHullmods.STORM_SKILL_MOD) }
        data.fleet.fleetData?.membersListCopy?.forEach { it.variant?.removeMod(RoiderHullmods.STORM_SKILL_MOD) }
    }

    override fun onDeactivation(data: SCData?) {
        if (data?.fleet?.stats == null) return
        modifyEBurn(data.fleet.stats)
        data.fleet.fleetData?.membersListCopy?.forEach { it.variant?.removeMod(RoiderHullmods.STORM_SKILL_MOD) }
    }

    private fun modifyEBurn(stats: MutableFleetStatsAPI) {
        if (stats.fleetwideMaxBurnMod?.flatBonuses?.contains("emergency_burn_ability_mod") == true) {
            stats.fleetwideMaxBurnMod?.modifyFlat(id, EMERGENCY_BURN_BONUS, ExternalStrings.STORM_SKILL_MOD_DESC)
        } else {
            stats.fleetwideMaxBurnMod?.unmodifyFlat(id)
        }
    }
}