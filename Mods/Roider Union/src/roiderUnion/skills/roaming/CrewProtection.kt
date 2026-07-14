package roiderUnion.skills.roaming

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.SkillsHelper
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class CrewProtection : SCBaseSkillPlugin() {
    companion object {
        const val CREW_PROTECTION = 0.5f
        const val CREW_PROTECTION_DISPLAY = "50"
        val ARMOR_BONUS = mapOf(
            Pair(HullSize.FIGHTER, 10f),
            Pair(HullSize.FRIGATE, 20f),
            Pair(HullSize.DESTROYER, 30f),
            Pair(HullSize.CRUISER, 40f),
            Pair(HullSize.CAPITAL_SHIP, 50f)
        )
        val ARMOR_BONUS_DISPLAY = ARMOR_BONUS[HullSize.FIGHTER]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                ARMOR_BONUS[HullSize.FRIGATE]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                ARMOR_BONUS[HullSize.DESTROYER]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                ARMOR_BONUS[HullSize.CRUISER]!!.toInt().toString() + ExternalStrings.LIST_DIVIDER +
                ARMOR_BONUS[HullSize.CAPITAL_SHIP]!!.toInt().toString()

        private const val BONUS_TOKEN = "[BONUS]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_ALL_SHIPS_FIGHTERS

    override fun addTooltip(p0: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.CREW_PROTECTION_LOSSES.replace(BONUS_TOKEN, CREW_PROTECTION_DISPLAY))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.CREW_PROTECTION_ARMOR_BONUS.replace(BONUS_TOKEN, ARMOR_BONUS_DISPLAY))
    }

    override fun applyEffectsBeforeShipCreation(
        data: SCData?,
        stats: MutableShipStatsAPI?,
        variant: ShipVariantAPI?,
        hullSize: HullSize?,
        id: String?
    ) {
        stats?.crewLossMult?.modifyMult(id, CREW_PROTECTION)
        stats?.effectiveArmorBonus?.modifyFlat(id, ARMOR_BONUS[hullSize] ?: 0f)
        stats?.dynamic?.getStat(Stats.FIGHTER_CREW_LOSS_MULT)?.modifyMult(id, CREW_PROTECTION)
    }

    override fun applyEffectsToFighterSpawnedByShip(data: SCData?, fighter: ShipAPI?, ship: ShipAPI?, id: String?) {
        fighter?.mutableStats?.effectiveArmorBonus?.modifyFlat(id, ARMOR_BONUS[HullSize.FIGHTER] ?: 0f)
    }
}