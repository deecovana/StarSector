package roiderUnion.skills.roaming

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIds
import second_in_command.SCData
import second_in_command.specs.SCAptitudeSection
import second_in_command.specs.SCBaseAptitudePlugin

class AptitudeRoaming : SCBaseAptitudePlugin() {
    override fun getOriginSkillId(): String = RoiderIds.Skills.NOMADIC

    override fun addCodexDescription(tooltip: TooltipMakerAPI?) {
        tooltip?.addPara(ExternalStrings.ROAMING_DESC, 0f, Misc.getTextColor(), Misc.getHighlightColor(), *ExternalStrings.ROAMING_HL.toTypedArray())
    }

    override fun createSections() {
        val section1 = SCAptitudeSection(true, 0, "technology1")
        section1.addSkill(RoiderIds.Skills.SALVAGING)
        section1.addSkill(RoiderIds.Skills.HAULING)
        section1.addSkill(RoiderIds.Skills.RINGER_MODS)
        section1.addSkill(RoiderIds.Skills.ROCKPIPER_ENGINEERS)
        section1.addSkill(RoiderIds.Skills.CREW_PROTECTION)
        section1.addSkill(RoiderIds.Skills.INTERCEPTION)
        section1.addSkill(RoiderIds.Skills.RESOLVE)
        addSection(section1)

        val section2 = SCAptitudeSection(false, 3, "technology2")
        section2.addSkill(RoiderIds.Skills.HULKS)
        section2.addSkill(RoiderIds.Skills.STORM)
        addSection(section2)
    }


    override fun getMarketSpawnweight(market: MarketAPI?): Float {
        var weight = spec.spawnWeight
        if (market?.faction?.id == RoiderFactions.ROIDER_UNION) weight *= 3f
        else if (Helper.hasRoiders(market)) weight *= 2f
        return weight
    }

    override fun getNPCFleetSpawnWeight(data: SCData?, fleet: CampaignFleetAPI?)  : Float {
        if (fleet?.faction?.id == RoiderFactions.ROIDER_UNION) return Float.MAX_VALUE
        // roider fleets should have good chance
        return 0.16f
    }
}