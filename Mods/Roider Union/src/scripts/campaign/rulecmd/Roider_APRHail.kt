package scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc

/**
 * HailPlayer
 *
 * Equivalent to:
 * AddText "You're being hailed by the $faction $otherShipOrFleet." $faction.baseColor
 * $hailing = true 0
 *
 * The latter changes the "open comm link" text to a yellow "accept the comm request".
 *
 * Copyright 2015 Fractal Softworks, LLC
 */
class Roider_APRHail : BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: List<Misc.Token>,
        memoryMap: Map<String, MemoryAPI>
    ): Boolean {
        if (dialog == null) return false
        if (dialog.interactionTarget !is CampaignFleetAPI) return false
        val fleet = dialog.interactionTarget as CampaignFleetAPI
        var shipOrFleet = "ship"
        if (fleet.fleetData.membersListCopy.size > 1) {
            shipOrFleet = "fleet"
        }
        val faction = fleet.faction
        var factionName = faction.entityNamePrefix
        if (factionName == null || factionName.isEmpty()) {
            factionName = faction.displayName
        }

//		dialog.getTextPanel().addPara("You're being hailed by the " + factionName + " " + shipOrFleet + ".",
//									  faction.getBaseUIColor());
        dialog.textPanel.addPara(
            "The $factionName $shipOrFleet is broadcasting an offer to retrofit ships.", // extern
            faction.baseUIColor
        )
        fleet.memoryWithoutUpdate["\$hailing", true] = 0f
        return true
    }
}