package roiderUnion.retrofits.old

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.Highlights
import com.thoughtworks.xstream.XStream
import roiderUnion.retrofits.old.base.BaseRetrofitManager
import roiderUnion.retrofits.old.base.BaseRetrofitPlugin

/**
 * Author: SafariJohn
 */
class ShipworksRetrofitPlugin(
    originalPlugin: InteractionDialogPlugin?,
    manager: BaseRetrofitManager,
    memoryMap: Map<String, MemoryAPI>
) : BaseRetrofitPlugin(originalPlugin, manager, memoryMap) {
    companion object {
        fun aliasAttributes(x: XStream?) {}
    }

    override fun init(dialog: InteractionDialogAPI) {
        super.init(dialog)
        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
        Global.getSoundPlayer().playCustomMusic(1, 1, UnionHQRetrofitPlugin.RETROFIT_MUSIC, true)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (OptionId.PICK_TARGET == optionData) pickTarget()
        else if (OptionId.PICK_SHIPS == optionData) pickShips()
        else if (OptionId.PRIORITIZE.equals(optionData)) prioritize()
        else if (OptionId.CANCEL_SHIPS.equals(optionData)) cancelShips()
        else if (optionData is List<*>) {
            confirmRetrofits(optionData as List<FleetMemberAPI>)
        } else if (OptionId.CANCEL.equals(optionData)) {
            updateText()
            updateOptions()
        } else {
            text!!.clear()
            options!!.clearOptions()
            visual!!.fadeVisualOut()
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
            Global.getSoundPlayer().restartCurrentMusic()
            dialog!!.setPlugin(originalPlugin)
            originalPlugin?.optionSelected(null, "backToBar") // extern
        }
    }

    override val leaveOptionText: String
        get() = "Return"
    override val notAllowedRetrofitsTitle: String
        get() = "Blueprint Required"

    override fun getNotAllowedRetrofitTextHighlights(hullId: String?): Highlights {
        val h = Highlights()
        h.setText(getNotAllowedRetrofitText(hullId))
        return h
    }

    override fun getNotAllowedRetrofitText(hullId: String?): String {
        return "You do not know how to retrofit this hull" // extern
    }

    override val isAllowed: Boolean
        get() = Global.getSector().playerFaction.knowsShip(selectedRetrofit?.getHullId())

    override fun isAllowed(sourceHull: String): Boolean {
        for (data in retrofits) {
            if (data.targetHull == selectedRetrofit?.getHullId() && matchesHullId(sourceHull, data.sourceHull)) {
                return Global.getSector().playerFaction.knowsShip(data.targetHull)
            }
        }
        return false
    }
}