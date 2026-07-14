package roiderUnion.retrofits.old

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.lwjgl.input.Keyboard
import roiderUnion.retrofits.old.base.BaseRetrofitPlugin

/**
 * Author: SafariJohn
 */
class UnionHQRetrofitPlugin  //    private final FactionAPI indies;
//    private PersonAPI person;
    (
    originalPlugin: InteractionDialogPlugin?,
    manager: UnionHQRetrofitManager, memoryMap: Map<String, MemoryAPI>
) : BaseRetrofitPlugin(originalPlugin, manager, memoryMap) {
    companion object {
        const val RETROFIT_MUSIC = "roider_retrofit"
        fun aliasAttributes(x: XStream?) {}
    }

    @JvmOverloads
    override fun init(dialog: InteractionDialogAPI) {
//        this.person = person;
        super.init(dialog)
        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
        Global.getSoundPlayer().playCustomMusic(1, 1, RETROFIT_MUSIC, true)
    }

    override fun updateOptions() {
        options!!.clearOptions()
        options!!.addOption("Pick retrofit hull", OptionId.PICK_TARGET) // extern

        // Queue text
        var queue = "Retrofit ships"
        if (manager.queued.isNotEmpty()) queue = "Queue retrofits"
        options!!.addOption(queue, OptionId.PICK_SHIPS)
        if (manager.queued.isNotEmpty()) {
            options!!.addOption("Prioritize retrofit", OptionId.PRIORITIZE)
            options!!.addOption("Cancel retrofits", OptionId.CANCEL_SHIPS)
        }
        options!!.addOption("Return", OptionId.LEAVE)
        options!!.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true)
        if (retrofits.isEmpty()) {
            // Should never happen normally.
            options!!.setEnabled(OptionId.PICK_TARGET, false)
            options!!.setTooltip(OptionId.PICK_TARGET, "No possible retrofits!")
        }
        if (selectedRetrofit == null) {
            options!!.setTooltip(OptionId.PICK_SHIPS, "Please select a hull to retrofit to.") // extern
            options!!.setEnabled(OptionId.PICK_SHIPS, false)
        } else {
            // Check if retrofit is blocked by reputation or commission
            if (isIllegal) {
                options!!.setTooltip(OptionId.PICK_SHIPS, getIllegalRetrofitText(null))
                options!!.setTooltipHighlightColors(
                    OptionId.PICK_SHIPS,
                    Misc.getNegativeHighlightColor(),
                    Misc.getNegativeHighlightColor()
                )
                options!!.setTooltipHighlights(OptionId.PICK_SHIPS, *getIllegalRetrofitTextHighlights(null)?.getText())
                options!!.setEnabled(OptionId.PICK_SHIPS, false)
            } else  // and that there are ships available
                if (availableShips.isEmpty()) {
                    options!!.setTooltip(
                        OptionId.PICK_SHIPS,
                        "You have no ships that can retrofit to " + selectedRetrofit // extern
                            ?.getHullSpec()?.getHullNameWithDashClass() + "."
                    )
                    options!!.setEnabled(OptionId.PICK_SHIPS, false)
                } else {
                    options!!.setEnabled(OptionId.PICK_SHIPS, true)
                }
        }
        val queueEmpty: Boolean = !manager.queued.isEmpty()
        options!!.setEnabled(OptionId.PRIORITIZE, queueEmpty)
        options!!.setEnabled(OptionId.CANCEL_SHIPS, queueEmpty)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (OptionId.PICK_TARGET.equals(optionData)) pickTarget()
        else if (OptionId.PICK_SHIPS.equals(optionData)) pickShips()
        else if (OptionId.PRIORITIZE.equals(optionData)) prioritize()
        else if (OptionId.CANCEL_SHIPS.equals(optionData)) cancelShips()
        else if (optionData is List<*>) {
            confirmRetrofits((optionData as List<FleetMemberAPI>))
        } else if (OptionId.CANCEL.equals(optionData)) {
            updateText()
            updateOptions()
        } else {
            text!!.clear()
            //            text!!.addPara("You finish your retrofitting arrangements.");
//            text!!.addPara("\"Anything else I can do for you?\"");
            options!!.clearOptions()
            visual!!.fadeVisualOut()
            //            visual!!.showPersonInfo(person);
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
            Global.getSoundPlayer().restartCurrentMusic()
            dialog!!.setPlugin(originalPlugin)
            originalPlugin?.optionSelected(null, "backToBar") // extern

//            FireAll.fire(null, dialog, memoryMap, "PopulateOptions");
        }
    } //    /**

    //     * Checks for selectedRetrofit.
    //     * @return
    //     */
    //    @Override
    //	protected boolean isIllegal() {
    //		RepLevel req = getRequiredLevel();
    //		if (req == null) return false;
    //
    //		RepLevel level = indies.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
    //
    //		boolean legal = level.isAtWorst(req);
    //		if (requiresCommission()) {
    //			legal &= hasCommission();
    //		}
    //
    //		return !legal;
    //	}
    //    /**
    //     * @param sourceHull
    //     * @return
    //     */
    //    @Override
    //	protected boolean isIllegal(String sourceHull) {
    //		RepLevel req = getRequiredLevel(sourceHull);
    //		if (req == null) return false;
    //
    //		RepLevel level = indies.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
    //
    //		boolean legal = level.isAtWorst(req);
    //		if (requiresCommission(sourceHull)) {
    //			legal &= hasCommission();
    //		}
    //
    //		return !legal;
    //	}
    //    @Override
    //	protected String getIllegalRetrofitText(String hullId) {
    //		RepLevel req;
    //        if (hullId == null) req = getRequiredLevel();
    //        else req = getRequiredLevel(hullId);
    //
    //		if (req != null) {
    //			String str = "";
    //			RepLevel level = indies.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
    //            if (hullId == null) {
    //                if (!level.isAtWorst(req)) {
    //                    str += "Req: " + indies.getDisplayName() + " - " + req.getDisplayName().toLowerCase();
    //                }
    //
    //                if (requiresCommission() && !hasCommission()) {
    //                    if (!str.isEmpty()) str += "\n";
    //                    str += "Req: " + manager.getFaction().getDisplayName() + " - " + "commission";
    //                }
    //            } else {
    //                if (!level.isAtWorst(req)) {
    //                    str += "- Req: " + indies.getDisplayName() + " - " + req.getDisplayName().toLowerCase();
    //                }
    //
    //                if (requiresCommission(hullId) && !hasCommission()) {
    //                    if (!str.isEmpty()) str += "\n";
    //                    str += "- Req: " + manager.getFaction().getDisplayName() + " - " + "commission";
    //                }
    //            }
    //
    //			return str;
    //		}
    //
    //        return null;
    //	}
    //    @Override
    //	protected Highlights getIllegalRetrofitTextHighlights(String hullId) {
    //		RepLevel req;
    //        if (hullId == null) req = getRequiredLevel();
    //        else req = getRequiredLevel(hullId);
    //
    //		if (req != null) {
    //			Color c = Misc.getNegativeHighlightColor();
    //			Highlights h = new Highlights();
    //			RepLevel level = indies.getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
    //            if (hullId == null) {
    //                if (!level.isAtWorst(req)) {
    //                    h.append("Req: " + indies.getDisplayName() + " - " + req.getDisplayName().toLowerCase(), c);
    //                }
    //
    //                if (requiresCommission() && !hasCommission()) {
    //                    h.append("Req: " + manager.getFaction().getDisplayName() + " - commission", c);
    //                }
    //            } else {
    //                if (!level.isAtWorst(req)) {
    //                    h.append("- Req: " + indies.getDisplayName() + " - " + req.getDisplayName().toLowerCase(), c);
    //                }
    //
    //                if (requiresCommission(hullId) && !hasCommission()) {
    //                    h.append("- Req: " + manager.getFaction().getDisplayName() + " - commission", c);
    //                }
    //            }
    //
    //			return h;
    //		}
    //
    //		return null;
    //	}
    //    @Override
    //	protected boolean hasCommission() {
    //        boolean roiderCommission = Global.getSector().getCharacterData().getMemoryWithoutUpdate()
    //                    .getBoolean(Roider_MemFlags.ROIDER_COMMISSION);
    //        return Factions.INDEPENDENT.equals(Misc.getCommissionFactionId()) && roiderCommission;
    //    }
}