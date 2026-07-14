package roiderUnion.nomads

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.DevMenuOptions
import com.fs.starfarer.api.impl.campaign.abilities.TransponderAbility
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.DumpMemory
import com.fs.starfarer.api.impl.campaign.tutorial.TutorialMissionIntel
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import roiderUnion.helpers.Helper
import java.util.*
import kotlin.math.roundToInt

class NomadJPWarningPlugin : InteractionDialogPlugin {

    val UNSTABLE_KEY = "\$unstable"
    val CAN_STABILIZE = "\$canStabilize"

    enum class OptionId {
        INIT, JUMP_1, JUMP_2, JUMP_3, JUMP_4, JUMP_5, JUMP_6, JUMP_7, JUMP_8, JUMP_9, JUMP_CONFIRM_TURN_TRANSPONDER_ON, JUMP_CONFIRM, STABILIZE, LEAVE
    }

    private var dialog: InteractionDialogAPI? = null
    private var textPanel: TextPanelAPI? = null
    private var options: OptionPanelAPI? = null
    private lateinit var visual: VisualPanelAPI

    private val playerFleet: CampaignFleetAPI
        get() = Helper.sector!!.playerFleet
    private var jumpPoint: JumpPointAPI? = null

    protected var shownConfirm = false
    protected var canAfford = false

    protected var beingConfirmed: OptionId? = null

    protected val jumpOptions = Arrays.asList(
        *arrayOf(
            OptionId.JUMP_1,
            OptionId.JUMP_2,
            OptionId.JUMP_3,
            OptionId.JUMP_4,
            OptionId.JUMP_5,
            OptionId.JUMP_6,
            OptionId.JUMP_7,
            OptionId.JUMP_8,
            OptionId.JUMP_9
        )
    )


    private val HIGHLIGHT_COLOR = Global.getSettings().getColor("buttonShortcut")

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        textPanel = dialog.textPanel
        options = dialog.optionPanel
        visual = dialog.visualPanel
        jumpPoint = dialog.interactionTarget as JumpPointAPI
        fuelCost = playerFleet.logistics.fuelCostPerLightYear
        var rounded = fuelCost.roundToInt().toFloat()
        if (fuelCost > 0 && rounded <= 0) rounded = 1f
        fuelCost = rounded
        if (jumpPoint!!.isInHyperspace) {
            fuelCost = 0f
        }
        canAfford = fuelCost <= playerFleet.cargo.fuel
        visual.setVisualFade(0.25f, 0.25f)
        if (jumpPoint!!.customInteractionDialogImageVisual != null) {
            visual.showImageVisual(jumpPoint!!.customInteractionDialogImageVisual)
        } else {
            if (playerFleet.containingLocation.isHyperspace) {
                visual.showImagePortion("illustrations", "jump_point_hyper", 640f, 400f, 0f, 0f, 480f, 300f)
            } else {
                visual.showImagePortion("illustrations", "jump_point_normal", 640f, 400f, 0f, 0f, 480f, 300f)
            }
        }

//		dialog.hideVisualPanel();
//		dialog.setTextWidth(1000);
        dialog.setOptionOnEscape("Leave", OptionId.LEAVE)
        optionSelected(null, OptionId.INIT)
    }

    override fun getMemoryMap(): Map<String?, MemoryAPI?>? {
        return null
    }

    private val lastResult: EngagementResultAPI? = null
    override fun backFromEngagement(result: EngagementResultAPI?) {
        // no combat here, so this won't get called
    }

    override fun optionSelected(text: String?, optionData: Any?) {
        if (optionData == null) return
        if (DumpMemory.OPTION_ID === optionData) {
            val memoryMap: MutableMap<String, MemoryAPI> = HashMap()
            val memory = dialog!!.interactionTarget.memory
            memoryMap[MemKeys.LOCAL] = memory
            if (dialog!!.interactionTarget.faction != null) {
                memoryMap[MemKeys.FACTION] = dialog!!.interactionTarget.faction.memory
            } else {
                memoryMap[MemKeys.FACTION] = Global.getFactory().createMemory()
            }
            memoryMap[MemKeys.GLOBAL] = Global.getSector().memory
            memoryMap[MemKeys.PLAYER] = Global.getSector().characterData.memory
            if (dialog!!.interactionTarget.market != null) {
                memoryMap[MemKeys.MARKET] = dialog!!.interactionTarget.market.memory
            }
            DumpMemory().execute(null, dialog, null, memoryMap)
            return
        } else if (DevMenuOptions.isDevOption(optionData)) {
            DevMenuOptions.execute(dialog, optionData as String?)
            return
        }
        val option = optionData as OptionId
        if (text != null) {
            //textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
            dialog!!.addOptionSelectedText(option)
        }
        val unstable = jumpPoint!!.memoryWithoutUpdate.getBoolean(UNSTABLE_KEY)
        val stabilizing = jumpPoint!!.memoryWithoutUpdate.getExpire(UNSTABLE_KEY) > 0
        val canStabilize = jumpPoint!!.memoryWithoutUpdate.getBoolean(CAN_STABILIZE)
        val canTransverseJump = Global.getSector().playerFleet.hasAbility(Abilities.TRANSVERSE_JUMP)
        val tutorialInProgress = TutorialMissionIntel.isTutorialInProgress()
        when (option) {
            OptionId.INIT -> {
                //			dialog.showCustomDialog(600, 400, new CustomDialogDelegate() {
//				public boolean hasCancelButton() {
//					return false;
//				}
//				public CustomUIPanelPlugin getCustomPanelPlugin() {
//					return new ExampleCustomUIPanel();
//				}
//				public String getConfirmText() {
//					return null;
//				}
//				public String getCancelText() {
//					return null;
//				}
//				public void customDialogConfirm() {
//					System.out.println("CUSTOM Confirmed");
//				}
//				public void customDialogCancel() {
//					System.out.println("CUSTOM Cancelled");
//				}
//				public void createCustomDialog(CustomPanelAPI panel) {
//					TooltipMakerAPI text = panel.createUIElement(600f, 200f, true);
//					for (int i = 0; i < 10; i++) {
//						text.addPara("The large amount of kinetic energy delivered to shield systems of enemy craft at close-range typically causes emitter overload, a tactical option often overlooked by inexperienced captains.", 10f);
//					}
//					panel.addUIElement(text).inTL(0, 0);
//				}
//			});
                addText(getString("approach"))
                val desc = Global.getSettings().getDescription(
                    jumpPoint!!.customDescriptionId, Description.Type.CUSTOM
                )
                if (desc != null && desc.hasText3()) {
                    addText(desc.text3)
                }
                if (unstable) {
                    if (stabilizing && !canTransverseJump) {
                        if (tutorialInProgress) {
                            addText("This jump-point is stabilizing and should be usable within a day at the most.")
                        } else {
                            addText("This jump-point is stabilizing but will not be usable for some time.")
                        }
                    } else {
                        addText("This jump-point is unstable and can not be used.")
                    }
                    if (canTransverseJump && !tutorialInProgress) {
                        addText("Until it restabilizes, hyperspace is only accessible via Transverse Jump.")
                    }
                } else {
                    if (!jumpPoint!!.isInHyperspace) {
                        if (canAfford) {
                            textPanel!!.addParagraph("Activating this jump point to let your fleet pass through will cost " + fuelCost.toInt() + " fuel.")
                            textPanel!!.highlightInLastPara(Misc.getHighlightColor(), "" + fuelCost.toInt())
                        } else {
                            val fuel = playerFleet.cargo.fuel.toInt()
                            if (fuel == 0) {
                                textPanel!!.addParagraph("Activating this jump point to let your fleet pass through will cost " + fuelCost.toInt() + " fuel. You have no fuel.")
                            } else {
                                textPanel!!.addParagraph("Activating this jump point to let your fleet pass through will cost " + fuelCost.toInt() + " fuel. You only have " + fuel + " fuel.")
                            }
                            textPanel!!.highlightInLastPara(
                                Misc.getNegativeHighlightColor(),
                                "" + fuelCost.toInt(),
                                "" + fuel
                            )
                        }
                    }
                    if (canAfford) {
                        showWarningIfNeeded()
                    }
                }
                createInitialOptions()
            }

            OptionId.STABILIZE -> {
                jumpPoint!!.memoryWithoutUpdate.unset(CAN_STABILIZE)
                //jumpPoint.getMemoryWithoutUpdate().unset(UNSTABLE_KEY);
                jumpPoint!!.memoryWithoutUpdate.expire(UNSTABLE_KEY, 1f)
                addText(
                    "You load the stabilization algorithm into your jump program and the drive field goes through a series " +
                            "of esoteric fluctuations, their resonance gradually cancelling out the instability in this jump-point."
                )
                addText("The jump-point should be stable enough to use within a day or so.")
                createInitialOptions()
            }

            OptionId.JUMP_CONFIRM_TURN_TRANSPONDER_ON -> {
                val t = Global.getSector().playerFleet.getAbility(Abilities.TRANSPONDER)
                if (t != null && !t.isActive) {
                    t.activate()
                }
                optionSelected(null, beingConfirmed)
            }

            OptionId.JUMP_CONFIRM -> optionSelected(null, beingConfirmed)
            OptionId.LEAVE -> {
                Global.getSector().campaignUI.isFollowingDirectCommand = true
                Global.getSector().isPaused = false
                dialog!!.dismiss()
            }
            else -> {}
        }
        if (jumpOptions.contains(option)) {
            val dest = destinationMap[option]
            if (dest != null) {
                if (!shownConfirm) {
                    val target = dest.destination
                    val player = Global.getSector().playerFleet
                    if ((target != null) && target.containingLocation != null &&
                        !target.containingLocation.isHyperspace && !player.isTransponderOn
                    ) {
                        val wouldBecomeHostile = TransponderAbility.getFactionsThatWouldBecomeHostile(player)
                        var wouldMindTOff = false
                        var isPopulated = false
                        for (market: MarketAPI in Global.getSector().economy.getMarkets(target.containingLocation)) {
                            if (market.isHidden && !NomadsHelper.bases.any { market === it.market }) continue
                            if (market.faction.isPlayerFaction) continue
                            isPopulated = true
                            if (!market.faction.isHostileTo(Factions.PLAYER) &&
                                !market.isFreePort &&
                                !market.faction.getCustomBoolean(Factions.CUSTOM_ALLOWS_TRANSPONDER_OFF_TRADE)
                            ) {
                                wouldMindTOff = true
                            }
                        }
                        if (isPopulated) {
                            if (wouldMindTOff) {
                                textPanel!!.addPara(
                                    "Your transponder is off, and patrols " +
                                            "in the " +
                                            target.containingLocation.nameWithLowercaseType +
                                            " are likely to give you trouble over the fact, if you're spotted."
                                )
                            } else {
                                textPanel!!.addPara(
                                    ("Your transponder is off, but any patrols in the " +
                                            target.containingLocation.nameWithLowercaseType +
                                            " are unlikely to raise the issue.")
                                )
                            }
                            if (!wouldBecomeHostile.isEmpty()) {
                                var str = "Turning the transponder on now would reveal your hostile actions to"
                                val first = true
                                var last = false
                                for (faction: FactionAPI in wouldBecomeHostile) {
                                    last = wouldBecomeHostile.indexOf(faction) == wouldBecomeHostile.size - 1
                                    if (first || !last) {
                                        str += " " + faction.displayNameWithArticle + ","
                                    } else {
                                        str += " and " + faction.displayNameWithArticle + ","
                                    }
                                }
                                str = str.substring(0, str.length - 1) + "."
                                textPanel!!.addPara(str, Misc.getNegativeHighlightColor())
                            }
                            options!!.clearOptions()
                            options!!.addOption(
                                "Turn the transponder on and then jump",
                                OptionId.JUMP_CONFIRM_TURN_TRANSPONDER_ON,
                                null
                            )
                            options!!.addOption("Jump, keeping the transponder off", OptionId.JUMP_CONFIRM, null)
                            beingConfirmed = option
                            options!!.addOption("Abort the jump", OptionId.LEAVE, null)
                            options!!.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true)
                            shownConfirm = true
                            return
                        }
                    }
                }


//				SectorEntityToken token = dest.getDestination();
//				//System.out.println("JUMP SELECTED");
//				LocationAPI destLoc = token.getContainingLocation();
//				LocationAPI curr = playerFleet.getContainingLocation();
//
//				Global.getSector().setCurrentLocation(destLoc);
//				curr.removeEntity(playerFleet);
//				destLoc.addEntity(playerFleet);
//
//				Global.getSector().setPaused(false);
//				playerFleet.setLocation(token.getLocation().x, token.getLocation().y);
//				playerFleet.setMoveDestination(token.getLocation().x, token.getLocation().y);
                if (Global.getSector().uiData.courseTarget === dialog!!.interactionTarget) {
                    Global.getSector().campaignUI.clearLaidInCourse()
                }
                dialog!!.dismiss()
                Global.getSector().isPaused = false
                Global.getSector().doHyperspaceTransition(playerFleet, jumpPoint, dest)
                playerFleet.cargo.removeFuel(fuelCost)
                return
            }
        }
    }

    protected fun showWarningIfNeeded() {
        if (jumpPoint!!.destinations.isEmpty()) return
        val dest = jumpPoint!!.destinations[0]
        val target = dest.destination
        if (target == null || target.containingLocation == null) return
        val fleets: MutableList<CampaignFleetAPI> = ArrayList()
        var hostile = false
        var minDist = Float.MAX_VALUE
        var maxDanger = 0
        for (other: CampaignFleetAPI in target.containingLocation.fleets) {
            val dist = Misc.getDistance(target, other)
            if (dist < 1000) {
                fleets.add(other)
                if (other.ai != null) {
                    hostile = hostile or other.ai.isHostileTo(Global.getSector().playerFleet)
                } else {
                    hostile = hostile or other.faction.isHostileTo(Factions.PLAYER)
                }
                if (other.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_PIRATE)) {
                    hostile = true
                }
                if (hostile) {
                    maxDanger = Math.max(maxDanger, Misc.getDangerLevel(other))
                }
                if (dist < minDist) minDist = dist
            }
        }
        val text = dialog!!.textPanel
        if (maxDanger >= 2) {
            text.addPara("Warning!", Misc.getNegativeHighlightColor())
            Global.getSoundPlayer().playUISound("cr_playership_malfunction", 1f, 0.25f)
            var where = "a short distance away from the exit"
            var whereHL: String = ""
            if (minDist < 300) {
                where = "extremely close to the exit"
                whereHL = where
            }
            text.addPara(
                ("The jump-point exhibits fluctuations " +
                        "characteristic of drive field activity " + where + "."),
                Misc.getNegativeHighlightColor(), whereHL
            )
            text.addPara(
                "A disposable probe sends back a microburst of information: forces " +
                        "near the exit are assesed likely hostile and a possible threat to your fleet.",
                Misc.getNegativeHighlightColor(), "hostile", "threat"
            )
        }

//		if (!fleets.isEmpty()) {
//			if (hostile) {
//				text.addPara("Warning!", Misc.getNegativeHighlightColor());
//				Global.getSoundPlayer().playUISound("cr_playership_malfunction", 1f, 0.25f);
//			}
//
//			text.addPara("The jump-point exhibits fluctuations " +
//										  "characteristic of an active drive field on the other side.");
//			String where = "a short distance away from the exit";
//			String whereHL = "";
//			if (minDist < 500) {
//				where = "extremely close to the exit";
//				whereHL = where;
//			}
//			if (fleets.size() == 1) {
//				String size = "small";
//				if (fleets.get(0).getFleetPoints() >= 150) size = "very large";
//				else if (fleets.get(0).getFleetPoints() >= 100) size = "large";
//				else if (fleets.get(0).getFleetPoints() >= 50) size = "medium-sized";
//
//
//				text.addPara("A more detailed analysis indicates it's likely a " + size + " fleet is " + where + ".",
//						Misc.getNegativeHighlightColor(), whereHL);
//			} else {
//				text.addPara("A more detailed analysis indicates it's likely multiple fleets are " + where + ".",
//						Misc.getNegativeHighlightColor(), whereHL);
//			}
//
//
//
//			text.addPara("You order a disposable probe sent through, " +
//					"and it's able to transmit a micro-burst of information " +
//					"before being destroyed by the stresses of jump-space.");
//			if (hostile) {
//				if (fleets.size() == 1) {
//					text.addPara("The probe's onboard algorithms estimate it's extremely likely " +
//							 "the nearby fleet is hostile.",
//							 Misc.getNegativeHighlightColor(), "hostile");
//				} else {
//					text.addPara("The probe's onboard algorithims estimate it's extremely likely " +
//							 "the nearby fleets include hostiles.",
//							 Misc.getNegativeHighlightColor(), "hostiles");
//				}
//			} else {
//				if (fleets.size() == 1) {
//					text.addPara("The probe's onboard algorithims estimate the nearby fleet is not a danger.");
//				} else {
//					text.addPara("The probe's onboard algorithims estimate the nearby fleets are not a danger.");
//				}
//			}
//		}
    }

    private val destinationMap: MutableMap<OptionId, JumpDestination> = HashMap()
    private fun createInitialOptions() {
        options!!.clearOptions()
        val dev = Global.getSettings().isDevMode
        val navigation = Global.getSector().playerFleet.commanderStats.getSkillLevel("navigation")
        val isStarAnchor = jumpPoint!!.isStarAnchor
        var okToUseIfAnchor = isStarAnchor && navigation >= 7
        okToUseIfAnchor = true
        if (isStarAnchor && !okToUseIfAnchor && dev) {
            addText("(Can always be used in dev mode)")
        }
        okToUseIfAnchor = okToUseIfAnchor or dev
        val unstable = jumpPoint!!.memoryWithoutUpdate.getBoolean(UNSTABLE_KEY)
        val canStabilize = jumpPoint!!.memoryWithoutUpdate.getBoolean(CAN_STABILIZE)
        if (unstable) {
            if (canStabilize) {
                options!!.addOption("Stabilize the jump-point", OptionId.STABILIZE, null)
            }
        } else {
            if (jumpPoint!!.destinations.isEmpty()) {
                addText(getString("noExits"))
            } else if (playerFleet.cargo.fuel <= 0 && !canAfford) {
                //addText(getString("noFuel"));
            } else if (isStarAnchor && !okToUseIfAnchor) {
                addText(getString("starAnchorUnusable"))
            } else if (canAfford) {
                var index = 0
                for (dest: JumpDestination in jumpPoint!!.destinations) {
                    if (index >= jumpOptions.size) break
                    val option = jumpOptions[index]
                    index++
                    options!!.addOption("Order a jump to " + dest.labelInInteractionDialog, option, null)
                    destinationMap[option] = dest
                }
            }
        }
        options!!.addOption("Leave", OptionId.LEAVE, null)
        options!!.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true)
        if (Global.getSettings().getBoolean("oneClickJumpPoints")) {
            if (jumpPoint!!.destinations.size == 1) {
                dialog!!.setOpacity(0f)
                dialog!!.setBackgroundDimAmount(0f)
                optionSelected(null, OptionId.JUMP_1)
            }
        }
        if (Global.getSettings().isDevMode) {
            DevMenuOptions.addOptions(dialog)
        }
    }


    protected var lastOptionMousedOver: OptionId? = null
    protected var fuelCost = 0f

    override fun optionMousedOver(optionText: String?, optionData: Any?) {}

    override fun advance(amount: Float) {}

    private fun addText(text: String) {
        textPanel!!.addParagraph(text)
    }

    private fun appendText(text: String) {
        textPanel!!.appendToLastParagraph(" $text")
    }

    private fun getString(id: String): String {
        var str = Global.getSettings().getString("jumpPointInteractionDialog", id)
        var fleetOrShip: String = "fleet"
        if (playerFleet.fleetData.membersListCopy.size == 1) {
            fleetOrShip = "ship"
            if (playerFleet.fleetData.membersListCopy[0].isFighterWing) {
                fleetOrShip = "fighter wing"
            }
        }
        str = str.replace("\\\$fleetOrShip".toRegex(), (fleetOrShip))
        return str
    }


    override fun getContext(): Any? {
        return null
    }
}