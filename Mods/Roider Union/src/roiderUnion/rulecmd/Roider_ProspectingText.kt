package roiderUnion.rulecmd

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.roidMining.RoidMiningHelper
import roiderUnion.roidMining.RoidPollResult
import roiderUnion.roidMining.RoidType

class Roider_ProspectingText : BaseCommandPlugin() {
    companion object {
        const val SUMMARY = "summary"
        const val BREAKDOWN = "breakdown"
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?,
    ): Boolean {
        if (true) return false
        if (Helper.anyNull(memoryMap, dialog?.interactionTarget, params)) return false
        val command = params!!.getOrNull(0)?.getString(memoryMap) ?: return false
        when (command) {
            SUMMARY -> printSummary(dialog!!)
            BREAKDOWN -> printBreakdown(dialog!!)
            else -> return false
        }

        return true
    }

    private fun printSummary(dialog: InteractionDialogAPI) {
        dialog.textPanel.clear()
        val playerFleet = Helper.sector?.playerFleet ?: return
        val text = dialog.textPanel ?: return
        val poll = RoidMiningHelper.pollRoids(playerFleet, dialog.interactionTarget!!)
        text.addPara("roid mining summary.")
        val totalPower = RoidMiningHelper.getMiningPower(playerFleet, null)
        text.addPara("cutting summary. base power: $totalPower")
        buildMiningEfficiencyTable(text.beginTooltip(), playerFleet)
        text.addTooltip()
        val minRoids = RoidMiningHelper.pollMinNumRoids(dialog.interactionTarget!!)
        val maxRoids = RoidMiningHelper.pollMaxNumRoids(dialog.interactionTarget!!)
        text.addPara("roid spawn summary - min: $minRoids, max: $maxRoids")
        buildProspectingTable(text.beginTooltip(), poll)
        text.addTooltip()
    }

    private fun buildMiningEfficiencyTable(tooltip: TooltipMakerAPI, fleet: CampaignFleetAPI) {
        tooltip.beginTable(fleet.faction, 20f, "Roid Type", 100f, "Efficiency", 100f, "Power", 100f)
        addMiningEfficiencyTableRow(tooltip, fleet, RoidType.ICE)
        addMiningEfficiencyTableRow(tooltip, fleet, RoidType.DUST)
        addMiningEfficiencyTableRow(tooltip, fleet, RoidType.ROCK)
        addMiningEfficiencyTableRow(tooltip, fleet, RoidType.METAL)
        addMiningEfficiencyTableRow(tooltip, fleet, RoidType.PLUTON)
        tooltip.addTable("", 0, Helper.PAD)
        tooltip.addSpacer(Helper.PAD)
    }

    private fun addMiningEfficiencyTableRow(tooltip: TooltipMakerAPI, fleet: CampaignFleetAPI, type: RoidType) {
        val factionColor = Helper.sector?.getFaction(type.faction)?.color ?: return
        val eff = RoidMiningHelper.getMiningEfficiency(fleet, type)
        val effHl = Misc.getHighlightColor()
        val power = RoidMiningHelper.getMiningPower(fleet, type)
        val powerHl = Misc.getHighlightColor()
        tooltip.addRow(
            Alignment.LMID, factionColor, type.name,
            Alignment.MID, effHl, Helper.multToPercentString(eff),
            Alignment.MID, powerHl, power.toString()
        )
    }

    private fun buildProspectingTable(tooltip: TooltipMakerAPI, poll: RoidPollResult) {
        tooltip.beginTable(Helper.sector?.playerFaction, 20f, "Roid Type", 100f, "Chance", 100f, "Yield", 100f)
        addProspectingTableRow(tooltip, poll, RoidType.ICE)
        addProspectingTableRow(tooltip, poll, RoidType.DUST)
        addProspectingTableRow(tooltip, poll, RoidType.ROCK)
        addProspectingTableRow(tooltip, poll, RoidType.METAL)
        addProspectingTableRow(tooltip, poll, RoidType.PLUTON)
        addProspectingTableRow(tooltip, poll, RoidType.NULL)
        tooltip.addTable("", 0, Helper.PAD)
        tooltip.addSpacer(Helper.PAD)
    }

    private fun addProspectingTableRow(tooltip: TooltipMakerAPI, poll: RoidPollResult, type: RoidType) {
        val factionColor = Helper.sector?.getFaction(type.faction)?.color ?: return
        val chance = poll.percents[type] ?: return
        val value = poll.averageValues[type]?.toInt() ?: return
        val valueString = if (value == 0) "-" else value.toString() // extern
        tooltip.addRow(
            Alignment.LMID, factionColor, type.name,
            Alignment.MID, factionColor, Helper.multToPercentString(chance),
            Alignment.MID, factionColor, valueString
        )

    }

    private fun printBreakdown(dialog: InteractionDialogAPI) {
        dialog.textPanel.clear()
        dialog.textPanel?.addPara("test breakdown")
    }
}