package roiderUnion.skills.roaming

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.SkillsHelper
import second_in_command.SCData
import second_in_command.specs.SCBaseSkillPlugin

class Interception : SCBaseSkillPlugin() {
    companion object {
        const val WEAPON_RANGE_MOD = 100f
        const val PD_DAMAGE = 25f

        private const val WEAPON_RANGE_TOKEN = "[WEAPON RANGE]"
        private const val DAMAGE_TOKEN = "[DAMAGE PERCENT]"
    }

    override fun getAffectsString(): String = ExternalStrings.SIC_ALL_FIGHTERS

    override fun addTooltip(p0: SCData?, tooltip: TooltipMakerAPI?) {
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.INTERCEPTION_WEAPON_RANGE.replace(WEAPON_RANGE_TOKEN, WEAPON_RANGE_MOD.toInt().toString()))
        SkillsHelper.sicStandardTooltip(tooltip, ExternalStrings.INTERCEPTION_FIGHTER_DAMAGE.replace(DAMAGE_TOKEN, PD_DAMAGE.toInt().toString()))
    }

    override fun applyEffectsToFighterSpawnedByShip(data: SCData?, fighter: ShipAPI?, ship: ShipAPI?, id: String?) {
        fighter?.mutableStats?.ballisticWeaponRangeBonus?.modifyFlat(id, WEAPON_RANGE_MOD)
        fighter?.mutableStats?.energyWeaponRangeBonus?.modifyFlat(id, WEAPON_RANGE_MOD)

        fighter?.mutableStats?.damageToFighters?.modifyPercent(id, PD_DAMAGE)
        fighter?.mutableStats?.damageToMissiles?.modifyPercent(id, PD_DAMAGE)
    }

    override fun getNPCSpawnWeight(fleet: CampaignFleetAPI?): Float {
        return if (fleet?.fleetData?.membersListWithFightersCopy?.any { it.isFighterWing } == true) super.getNPCSpawnWeight(fleet)
        else 0f
    }
}