package roiderUnion.rulecmd

import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.Commission
import exerelin.campaign.AllianceManager
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIds
import roiderUnion.ModPlugin

/**
 * Author: SafariJohn
 */
class Roider_Commission : Commission() {
    override fun personCanGiveCommission(): Boolean {
        if (faction?.id != RoiderFactions.ROIDER_UNION) return false
        if (ModPlugin.hasNexerelin) {
            val ally = AllianceManager.getPlayerAlliance(false)
            if (ally != null) return false
        }

        //if (Misc.getCommissionFactionId() != null) return false;
        return RoiderIds.Roider_Ranks.POST_BASE_COMMANDER == person?.postId
    }

    override fun printInfo() {
        if (true) return
//        val info: TooltipMakerAPI = dialog.textPanel.beginTooltip()
//        val temp: FactionCommissionIntel = Roider_FactionCommissionIntel(faction)
//        val h: Color = Misc.getHighlightColor()
//        val g: Color = Misc.getGrayColor()
//        val pad = 3f
//        val opad = 10f
//        info.setParaSmallInsignia()
//        val stipend: Int = temp.computeStipend().toInt()
//        info.addPara(
//            "At your experience level, you would receive a %s monthly stipend, as well as a modest bounty for destroying enemy ships.", // extern
//            0f, h, Misc.getDGSCredits(stipend.toFloat())
//        )
//        val hostile: List<FactionAPI> = temp.hostileFactions
//        if (hostile.isEmpty()) {
//            info.addPara(
//                Misc.ucFirst(faction.displayNameWithArticle) + " is not currently hostile to any major factions.",
//                0f
//            )
//        } else {
//            info.addPara(Misc.ucFirst(faction.displayNameWithArticle) + " is currently hostile to:", opad)
//            info.setParaFontDefault()
//            info.setBulletedListMode(BaseIntelPlugin.INDENT)
//            var initPad = opad
//            for (other in hostile) {
//                info.addPara(Misc.ucFirst(other.displayName), other.baseUIColor, initPad)
//                initPad = 3f
//            }
//            info.setBulletedListMode(null)
//        }
//        dialog.textPanel.addTooltip()
    }

    override fun printRequirements() {
        CoreReputationPlugin.addRequiredStanding(
            faction,
            COMMISSION_REQ,
            null,
            dialog?.textPanel,
            null,
            null,
            0f,
            true
        )
        CoreReputationPlugin.addCurrentStanding(faction, null, dialog?.textPanel, null, null, 0f)
    }
}