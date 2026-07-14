package roiderUnion.retrofits

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.CombatReadinessPlugin
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Highlights
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.lwjgl.input.Keyboard
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.retrofits.old.base.BaseRetrofitManager
import roiderUnion.retrofits.old.base.BaseRetrofitPlugin
import roiderUnion.retrofits.old.base.RetrofitData
import java.awt.Color
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs

/**
 * Author: SafariJohn
 */
class ArgosRetrofitPlugin(
    originalPlugin: InteractionDialogPlugin?,
    manager: ArgosRetrofitManager_Old, memoryMap: Map<String, MemoryAPI>?
) : BaseRetrofitPlugin(originalPlugin, manager, memoryMap) {
    private val selectedDocks = ArrayList<FleetMemberAPI>()

    override fun updateOptions() {
        options!!.clearOptions()
        options!!.addOption("Pick retrofit hull", OptionId.PICK_TARGET)
        options!!.addOption("Retrofit ships", OptionId.PICK_SHIPS)
        options!!.addOption("Pick conversion docks", PICK_DOCKS)
        options!!.addOption(leaveOptionText, OptionId.LEAVE)
        options!!.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true)
        if (retrofits.isEmpty()) {
            // Should never happen normally.
            options!!.setEnabled(OptionId.PICK_TARGET, false)
            options!!.setTooltip(OptionId.PICK_TARGET, "No possible retrofits!")
        }
        if (selectedRetrofit == null) {
            options!!.setTooltip(OptionId.PICK_SHIPS, "Please select a hull to retrofit to.")
            options!!.setEnabled(OptionId.PICK_SHIPS, false)
        } else {
            if (!isAllowed) {
                options!!.setTooltip(OptionId.PICK_SHIPS, getNotAllowedRetrofitText(null))
                options!!.setTooltipHighlightColors(
                    OptionId.PICK_SHIPS,
                    Misc.getNegativeHighlightColor(),
                    Misc.getNegativeHighlightColor()
                )
                options!!.setTooltipHighlights(OptionId.PICK_SHIPS, getNotAllowedRetrofitTextHighlights(null).text[0])
                options!!.setEnabled(OptionId.PICK_SHIPS, false)
            } else if (isIllegal("")) { // Check if retrofit is blocked by reputation or commission
                options!!.setTooltip(OptionId.PICK_SHIPS, getIllegalRetrofitText(null))
                options!!.setTooltipHighlightColors(
                    OptionId.PICK_SHIPS,
                    Misc.getNegativeHighlightColor(),
                    Misc.getNegativeHighlightColor()
                )
                options!!.setTooltipHighlights(OptionId.PICK_SHIPS, getIllegalRetrofitTextHighlights(null)!!.text[0] ?: "")
                options!!.setEnabled(OptionId.PICK_SHIPS, false)
            } else  // and that there are ships available
                if (availableShips.isEmpty()) {
                    options!!.setTooltip(
                        OptionId.PICK_SHIPS,
                        "You have no ships that can retrofit to " + selectedRetrofit?.hullSpec
                            ?.hullNameWithDashClass + "."
                    )
                    options!!.setEnabled(OptionId.PICK_SHIPS, false)
                } else {
                    options!!.setEnabled(OptionId.PICK_SHIPS, true)
                }
        }
    }

    override val leaveOptionText: String
        get() {
            var leaveText = "Return"
            if (manager.faction.isPlayerFaction) leaveText = "Leave"
            return leaveText
        }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (OptionId.PICK_TARGET == optionData) pickTarget()
        else if (OptionId.PICK_SHIPS == optionData) pickShips()
        else if (OptionId.PRIORITIZE == optionData) prioritize()
        else if (OptionId.CANCEL_SHIPS == optionData) cancelShips()
        else if (optionData is List<*>) {
            confirmRetrofits(optionData as List<FleetMemberAPI>)
        } else if (OptionId.CANCEL == optionData) {
            updateText()
            updateOptions()
        } else if (PICK_DOCKS == optionData) {
            pickDocks()
        } else {
            text!!.clear()
            //            text!!.addPara("You finish your retrofitting arrangements.");
//            text!!.addPara("\"Anything else I can do for you?\"");
            options!!.clearOptions()
            visual!!.fadeVisualOut()
            //            visual.showPersonInfo(person);
            if (originalPlugin == null) {
                dialog!!.dismiss()
            } else {
                dialog!!.plugin = originalPlugin
                originalPlugin.optionSelected("Finished retrofitting", "roider_argosFinishedConverting")
            }
            Global.getSoundPlayer().pauseCustomMusic()
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)

//            FireAll.fire(null, dialog, memoryMap, "PopulateOptions");
        }
    }

    override fun updateVisual() {
        super.updateVisual()
        if (selectedRetrofit != null && selectedConversionDockShips.isNotEmpty()) return

        // Need to show available and active Argosi
        val active: CampaignFleetAPI =
            Global.getFactory().createEmptyFleet(manager.faction.id, "Active", true)
        for (dock in selectedConversionDockShips) {
            val cr: Float = dock.repairTracker.cr
            active.fleetData.addFleetMember(dock)
            dock.repairTracker.cr = cr
        }
        val avail: CampaignFleetAPI =
            Global.getFactory().createEmptyFleet(manager.faction.id, "Available", true)
        for (dock in readyConversionDockShips) {
            if (selectedConversionDockShips.contains(dock)) continue
            val cr: Float = dock.repairTracker.cr
            avail.fleetData.addFleetMember(dock)
            dock.repairTracker.cr = cr
        }
        visual!!.showFleetInfo("Active conversion docks", active, "Available conversion docks", avail) // extern
    }

    override fun updateText() {
        text!!.clear()
        if (!manager.faction.isPlayerFaction
            || selectedRetrofit == null
        ) {
            super.updateText()
            return
        }

        // Need to show resource requirements when player is converting
        text!!.addPara(
            "Retrofitting to: " + selectedRetrofit?.variant // extern
                ?.fullDesignationWithHullName, Misc.getButtonTextColor()
        )
        val costs = StringBuilder()
        val highlights: MutableList<String> = ArrayList()
        val illegalCosts = StringBuilder()
        var firstIllegal = true
        var firstLegal = true
        val matched: MutableList<String> = ArrayList()

        // Check for specific matches
        for (data in retrofits) {
            if (data.targetHull != selectedRetrofit?.hullId) continue
            for (spec in Global.getSettings().allShipHullSpecs) {
                if (matched.contains(spec.hullId)) continue
                if (spec.isDHull && !matchesHullId(spec.hullId, data.sourceHull)) continue
                if (spec.hullId == selectedRetrofit?.hullId) continue
                if (matchesHullId(spec.hullId, data.sourceHull)) {
                    if (!isAllowed(data.sourceHull)) {
                        if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                        illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                        appendIllegalsCosts(illegalCosts, getResourceCosts(selectedRetrofit, data.cost.toInt()))
                        illegalCosts.append("- ").append(getNotAllowedRetrofitText(data.sourceHull))
                    } else if (isIllegal(data.sourceHull)) {
                        if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                        illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                        appendIllegalsCosts(illegalCosts, getResourceCosts(selectedRetrofit, data.cost.toInt()))
                        illegalCosts.append(getIllegalRetrofitText(data.sourceHull))
                    } else {
                        if (firstLegal) firstLegal = false else costs.append("\n")
                        costs.append(spec.nameWithDesignationWithDashClass).append("\n")
                        appendLegalCosts(costs, highlights, getResourceCosts(selectedRetrofit, data.cost.toInt()))
                    }
                    matched.add(spec.hullId)
                    matched.add(spec.hullName)
                    break
                }
            }
        }

        // Check for general matches
        for (data in retrofits) {
            if (data.targetHull != selectedRetrofit?.hullId) continue
            for (spec in Global.getSettings().allShipHullSpecs) {
                if (matched.contains(spec.hullId)) continue
                if (matched.contains(spec.hullName)) continue
                if (spec.isDHull && !matchesHullId(spec.hullId, data.sourceHull)) continue
                if (spec.hullId == selectedRetrofit?.hullId) continue
                if (matchesHullId(spec, data.sourceHull)) {
                    if (!isAllowed(data.sourceHull)) {
                        if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                        illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                        appendIllegalsCosts(illegalCosts, getResourceCosts(selectedRetrofit, data.cost.toInt()))
                        illegalCosts.append("- ").append(getNotAllowedRetrofitText(data.sourceHull))
                    } else if (isIllegal(data.sourceHull)) {
                        if (firstIllegal) firstIllegal = false else illegalCosts.append("\n")
                        illegalCosts.append(spec.nameWithDesignationWithDashClass).append("\n")
                        appendIllegalsCosts(illegalCosts, getResourceCosts(selectedRetrofit, data.cost.toInt()))
                        illegalCosts.append(getIllegalRetrofitText(data.sourceHull))
                    } else {
                        if (firstLegal) firstLegal = false else costs.append("\n")
                        costs.append(spec.nameWithDesignationWithDashClass).append("\n")
                        appendLegalCosts(costs, highlights, getResourceCosts(selectedRetrofit, data.cost.toInt()))
                    }
                }
            }
        }
        if (costs.isNotEmpty()) text!!.addPara(
            costs.toString(),
            Misc.getHighlightColor()
        )
        if (illegalCosts.isNotEmpty()) text!!.addPara(
            illegalCosts.toString(),
            Misc.getNegativeHighlightColor(), illegalCosts.toString()
        )
    }

    private fun appendIllegalsCosts(costs: StringBuilder, resources: Map<String, Int>) {
        // CR cost
        costs.append("- Costs ").append(resources[COM_CR]).append(" CR\n") // extern
        val cost = resources[Commodities.CREDITS]!!
        if (cost > 0) {
            costs.append("- Costs ").append(resources[Commodities.HEAVY_MACHINERY]).append(" heavy machinery\n")
            costs.append("- Costs ").append(resources[Commodities.SUPPLIES]).append(" supplies\n")
            costs.append("- Costs ").append(resources[Commodities.METALS]).append(" metals\n")
        } else if (cost < 0) {
            costs.append("- Gives ").append(resources[Commodities.HEAVY_MACHINERY]).append(" heavy machinery\n")
            costs.append("- Gives ").append(resources[Commodities.SUPPLIES]).append(" supplies\n")
            costs.append("- Gives ").append(resources[Commodities.METALS]).append(" metals\n")
        } else {
            costs.append("- Free\n")
        }
    }

    private fun appendLegalCosts(costs: StringBuilder, highlights: MutableList<String>, resources: Map<String, Int>) {
        costs.append("- Costs %s CR\n")
        highlights.add(resources[COM_CR].toString() + "") // extern
        val cost = resources[Commodities.CREDITS]!!
        if (cost > 0) {
            costs.append("- Costs %s heavy machinery\n")
            highlights.add("" + resources[Commodities.HEAVY_MACHINERY])
            costs.append("- Costs %s supplies\n")
            highlights.add("" + resources[Commodities.SUPPLIES])
            costs.append("- Costs %s metals")
            highlights.add("" + resources[Commodities.METALS])
        } else if (cost < 0) {
            costs.append("- Gives %s heavy machinery\n")
            highlights.add("" + resources[Commodities.HEAVY_MACHINERY])
            costs.append("- Gives %s supplies\n")
            highlights.add("" + resources[Commodities.SUPPLIES])
            costs.append("- Gives %s metals")
            highlights.add("" + resources[Commodities.METALS])
        } else {
            highlights.add("Free")
        }
    }

    private fun getResourceCosts(targetHull: FleetMemberAPI?, credits: Int): Map<String, Int> {
        val resources: MutableMap<String, Int> = HashMap()
        resources[Commodities.CREDITS] = credits // Passing credit cost along

        // Costs are CR, supplies, metal, and heavy machinery

        // 25% of credit cost is heavy machinery
        val machinery = credits / Global.getSettings().getCommoditySpec(Commodities.HEAVY_MACHINERY).basePrice / 4f
        resources[Commodities.HEAVY_MACHINERY] = machinery.toInt()

        // 50% of credit cost is supplies
        val supplies = credits / Global.getSettings().getCommoditySpec(Commodities.SUPPLIES).basePrice / 2f
        resources[Commodities.SUPPLIES] = supplies.toInt()

        // 25% of credit cost is metals
        val metals = credits / Global.getSettings().getCommoditySpec(Commodities.METALS).basePrice / 4f
        resources[Commodities.METALS] = metals.toInt()

        // CR cost is on top of other costs
        resources[COM_CR] = targetHull?.deploymentCostSupplies?.toInt() ?: 0
        return resources
    }

    override val notAllowedRetrofitsTitle: String
        get() = "Blueprint Required"

    override fun getNotAllowedRetrofitTextHighlights(hullId: String?): Highlights {
        val h = Highlights()
        h.setText(getNotAllowedRetrofitText(hullId))
        return h
    }

    override fun getNotAllowedRetrofitText(hullId: String?): String {
        return "You do not know how to retrofit this hull"
    }

    override val isAllowed: Boolean
        get() = if (manager.faction.isPlayerFaction) {
            Global.getSector().playerFaction.knowsShip(selectedRetrofit?.hullId)
        } else true

    override fun isAllowed(sourceHull: String): Boolean {
        if (manager.faction.isPlayerFaction) {
            for (data in retrofits) {
                if (data.targetHull == selectedRetrofit?.hullId && matchesHullId(sourceHull, data.sourceHull)) {
                    return Global.getSector().playerFaction.knowsShip(data.targetHull)
                }
            }
            return false
        }
        return true
    }

    override fun showRetrofitConfirmDialog(members: List<FleetMemberAPI>) {
        if (members.isEmpty()) return

        // Tally costs
        var tCost = 0.0 // for player only
        val crCosts: MutableList<Float> = ArrayList()
        for (ship in members) {
            var data: RetrofitData? = null
            // Check for exact match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                        ship.hullSpec.hullId,
                        d.sourceHull
                    )
                ) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
                crCosts.add((selectedRetrofit?.deploymentCostSupplies ?: 0f) / 100f)
                continue
            }

            // Check for general match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(ship.hullSpec, d.sourceHull)) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
                crCosts.add((selectedRetrofit?.deploymentCostSupplies ?: 0f) / 100f)
            }
        }

        // If another faction is doing the converting
        if (!manager.faction.isPlayerFaction) {
            super.showRetrofitConfirmDialog(members)
            if (!docksHaveEnoughCR(crCosts)) {
                options!!.setEnabled(members, false)
                options!!.setTooltip(members, "Their conversion dock ships do not have enough CR!")
                options!!.setTooltipHighlights(members, "Their conversion dock ships do not have enough CR!")
                options!!.setTooltipHighlightColors(members, Misc.getNegativeHighlightColor())
            }
            return
        }

        // Else if player is converting
        /**
         * Sort CR list so largest CR costs go last because
         * the last deduction on a dock goes through as long as
         * the dock is above malfunction CR
         */
        crCosts.sort()
        val enoughCR = docksHaveEnoughCR(crCosts)
        val canAfford = enoughCR && canAffordResources(tCost)

        // Update text
        text!!.clear()
        val targetTooltip: TooltipMakerAPI = text!!.beginTooltip()
        targetTooltip.addTitle("Confirm Retrofits")
        val rows: Int = members.size / COLUMNS + 1
        val iconSize: Float = dialog!!.textWidth / COLUMNS
        val pad = 0f // 10f
        val color: Color = manager.faction.baseUIColor
        targetTooltip.addShipList(COLUMNS, rows, iconSize, color, members, pad)
        text!!.addTooltip()
        val rez = getResourceCosts(selectedRetrofit, abs(tCost).toInt())
        val totalResources = (rez[Commodities.HEAVY_MACHINERY]!!
                + rez[Commodities.SUPPLIES]!!
                + rez[Commodities.METALS]!!)
        var message = "It is free "
        if (tCost > 0) {
            message = "You must pay $totalResources resources" // extern
        } else if (tCost < 0) {
            message = "You will receive $totalResources resources"
        }
        message += " to convert"
        message += if (members.size == 1) " this ship. The new hull will be pristine." else " these ships. The new hulls will be pristine."
        text!!.addPara(message, Misc.getHighlightColor(), "$totalResources resources")
        Misc.showCost(
            text,
            color,
            manager.faction.darkUIColor,
            arrayOf(Commodities.HEAVY_MACHINERY, Commodities.SUPPLIES, Commodities.METALS),
            intArrayOf(
                rez[Commodities.HEAVY_MACHINERY]!!, rez[Commodities.SUPPLIES]!!, rez[Commodities.METALS]!!
            )
        )

        // Update options
        options!!.clearOptions()
        var confirmText = "Confirm"
        if (tCost > 0) {
            confirmText = "Pay $totalResources resources"
        } else if (tCost < 0) {
            confirmText = "Receive $totalResources resources"
        }
        options!!.addOption(confirmText, members)
        if (selectedDocks.isEmpty()) {
            options!!.setEnabled(members, false)
            options!!.setTooltip(members, "Please pick one or more conversion dock ships!")
            options!!.setTooltipHighlights(members, "Please pick one or more conversion dock ships!")
            options!!.setTooltipHighlightColors(members, Misc.getNegativeHighlightColor())
        } else if (!enoughCR) {
            options!!.setEnabled(members, false)
            options!!.setTooltip(members, "Your conversion dock ships do not have enough CR!")
            options!!.setTooltipHighlights(members, "Your conversion dock ships do not have enough CR!")
            options!!.setTooltipHighlightColors(members, Misc.getNegativeHighlightColor())
        } else if (!canAfford) {
            options!!.setEnabled(members, false)
            options!!.setTooltip(members, "You do not have enough resources!")
            options!!.setTooltipHighlights(members, "You do not have enough resources!")
            options!!.setTooltipHighlightColors(members, Misc.getNegativeHighlightColor())
        }
        options!!.addOption("Cancel", OptionId.CANCEL)
        options!!.setShortcut(OptionId.CANCEL, Keyboard.KEY_ESCAPE, false, false, false, true)
    }

    private fun docksHaveEnoughCR(crCosts: List<Float>): Boolean {
        val crAvail = HashMap<FleetMemberAPI, Float?>()
        for (dock in selectedConversionDockShips) {
            crAvail[dock] = dock.repairTracker.cr
        }
        val crPlugin: CombatReadinessPlugin = Global.getSettings().crPlugin
        val replace= HashMap<FleetMemberAPI, Float>()
        for (cr in crCosts) {
            var afforded = false
            for (dock in crAvail.keys) {
                val crA = crAvail[dock]!!
                if (crA > crPlugin.getMalfunctionThreshold(dock.stats)
                    && crA >= cr
                ) {
                    replace[dock] = crA - cr
                    afforded = true
                    break
                }
            }
            for (dock in replace.keys) {
                crAvail[dock] = replace[dock]
            }
            replace.clear()
            if (!afforded) {
                return false
            }
        }
        return true
    }

    private fun canAffordResources(tCost: Double): Boolean {
        if (tCost > 0) {
            val resources = getResourceCosts(selectedRetrofit, tCost.toInt())
            val cargo: CargoAPI = Global.getSector().playerFleet.cargo
            var com: String = Commodities.HEAVY_MACHINERY
            if (cargo.getCommodityQuantity(com) < resources[com]!!) return false
            com = Commodities.SUPPLIES
            if (cargo.getCommodityQuantity(com) < resources[com]!!) return false
            com = Commodities.METALS
            if (cargo.getCommodityQuantity(com) < resources[com]!!) return false
        }
        return true
    }

    override fun confirmRetrofits(members: List<FleetMemberAPI>) {
        // Tally cost
        var tCost = 0.0
        val crCosts: MutableList<Float> = ArrayList()
        for (ship in members) {
            var data: RetrofitData? = null
            // Check for exact match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                        ship.hullSpec.hullId,
                        d.sourceHull
                    )
                ) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
                crCosts.add((selectedRetrofit?.deploymentCostSupplies ?: 0f) / 100f)
                continue
            }

            // Check for general match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(ship.hullSpec, d.sourceHull)) {
                    data = d
                    break
                }
            }
            if (data != null) {
                tCost += data.cost
                crCosts.add((selectedRetrofit?.deploymentCostSupplies ?: 0f) / 100f)
            }
        }

        // Consume dock CR
        val docks = selectedConversionDockShips
        val crPlugin: CombatReadinessPlugin = Global.getSettings().crPlugin
        docks.sortWith { o1, o2 ->
            (o1.repairTracker.cr * 100f - o2.repairTracker.cr * 100f).toInt()
        }
        for (cr in crCosts) {
            for (dock in docks) {
                if (dock.repairTracker.cr > crPlugin.getMalfunctionThreshold(dock.stats)
                    && dock.repairTracker.cr >= cr
                ) {
                    dock.repairTracker.applyCREvent(-cr, "Converted ship")
                    break
                }
            }
            docks.sortWith { o1, o2 ->
                (o1.repairTracker.cr * 100f - o2.repairTracker.cr * 100f).toInt()
            }
        }

        // Need to deduct resources instead of credits if player is converting
        if (manager.faction.isPlayerFaction) {
            if (tCost > 0) {
                val resources = getResourceCosts(selectedRetrofit, tCost.toInt())
                var com: String = Commodities.HEAVY_MACHINERY
                Global.getSector().playerFleet.cargo.removeCommodity(
                    com, resources[com]!!
                        .toFloat()
                )
                Global.getSector().campaignUI.messageDisplay
                    .addMessage("Lost " + resources[com] + " heavy machinery for retrofits")
                com = Commodities.SUPPLIES
                Global.getSector().playerFleet.cargo.removeCommodity(
                    com, resources[com]!!
                        .toFloat()
                )
                Global.getSector().campaignUI.messageDisplay
                    .addMessage("Lost " + resources[com] + " supplies for retrofits")
                com = Commodities.METALS
                Global.getSector().playerFleet.cargo.removeCommodity(
                    com, resources[com]!!
                        .toFloat()
                )
                Global.getSector().campaignUI.messageDisplay
                    .addMessage("Lost " + resources[com] + " metals for retrofits")
            }
            if (tCost < 0) {
                val resources = getResourceCosts(selectedRetrofit, -tCost.toInt())
                var com: String = Commodities.HEAVY_MACHINERY
                Global.getSector().playerFleet.cargo.addCommodity(
                    com, resources[com]!!
                        .toFloat()
                )
                Global.getSector().campaignUI.messageDisplay
                    .addMessage("Gained " + resources[com] + " heavy machinery from retrofits")
                com = Commodities.SUPPLIES
                Global.getSector().playerFleet.cargo.addCommodity(
                    com, resources[com]!!
                        .toFloat()
                )
                Global.getSector().campaignUI.messageDisplay
                    .addMessage("Gained " + resources[com] + " supplies from retrofits")
                com = Commodities.METALS
                Global.getSector().playerFleet.cargo.addCommodity(
                    com, resources[com]!!
                        .toFloat()
                )
                Global.getSector().campaignUI.messageDisplay
                    .addMessage("Gained " + resources[com] + " metals from retrofits")
            }
        } else {
            // Charge player
            if (tCost > 0) {
                Global.getSector().playerFleet.cargo.credits.subtract(tCost.toFloat())
                Global.getSector().campaignUI.messageDisplay.addMessage("Paid $tCost for retrofits")
            }
            // Pay player
            if (tCost < 0) {
                Global.getSector().playerFleet.cargo.credits.add(-tCost.toFloat())
                Global.getSector().campaignUI.messageDisplay.addMessage("Received " + -tCost + " for retrofits")
            }
        }

        // Strip ships
        val playerCargo: CargoAPI = Global.getSector().playerFleet.cargo
        var stripped = false
        for (ship in members) {
            for (slot in ship.variant.nonBuiltInWeaponSlots) {
                if (ship.variant.getWeaponId(slot) == null || ship.variant.getWeaponId(slot)
                        .isEmpty()
                ) continue
                playerCargo.addWeapons(ship.variant.getWeaponId(slot), 1)
                stripped = true
                ship.variant.clearSlot(slot)
            }
            for (wing in ship.variant.fittedWings) {
                playerCargo.addFighters(wing, 1)
                stripped = true
            }
            ship.variant.fittedWings.clear()
        }
        if (stripped) Global.getSector().campaignUI.messageDisplay.addMessage("Stripped weapons and fighters to cargo")

        // Remove from player's fleet
        val playerFleet: CampaignFleetAPI = Global.getSector().playerFleet
        for (ship in members) {
            playerFleet.fleetData.removeFleetMember(ship)
        }

        // Add to queue
        for (ship in members) {
            var data: RetrofitData? = null
            // Check for exact match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(
                        ship.hullSpec.hullId,
                        d.sourceHull
                    )
                ) {
                    data = d
                    break
                }
            }
            if (data != null) {
                manager.addToQueue(BaseRetrofitManager.RetrofitTracker(ship, data, data.cost))
                continue
            }

            // Check for general match
            for (d in retrofits) {
                if (d.targetHull == selectedRetrofit?.hullId && matchesHullId(ship.hullSpec, d.sourceHull)) {
                    data = d
                    break
                }
            }
            if (data != null) {
                manager.addToQueue(BaseRetrofitManager.RetrofitTracker(ship, data, data.cost))
            }
        }
        updateText()
        updateOptions()
    }

    private fun pickDocks() {
        val avail: List<FleetMemberAPI> = readyConversionDockShips
        val rows = avail.size / 7 + 1
        dialog!!.showFleetMemberPickerDialog(
            "Pick ships with conversion docks to use",
            "Ok",
            "Cancel",
            rows,
            COLUMNS,
            58f,
            true,
            true,
            avail,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    selectedDocks.clear()
                    selectedDocks.addAll(members)
                    updateOptions()
                    updateVisual()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    private val readyConversionDockShips: List<FleetMemberAPI>
        get() {
            val fleet: CampaignFleetAPI = manager.entity as CampaignFleetAPI
            val crPlugin: CombatReadinessPlugin = Global.getSettings().crPlugin
            val docks = ArrayList<FleetMemberAPI>()
            for (ship in fleet.fleetData.membersListCopy) {
                if (ship.variant.hasHullMod(RoiderHullmods.CONVERSION_DOCK)) {
                    if (ship.isMothballed) continue
                    val mal: Float = crPlugin.getMalfunctionThreshold(ship.stats)
                    val cr: Float = ship.repairTracker.cr
                    if (cr <= mal) continue
                    docks.add(ship)
                }
            }
            return docks
        }
    private val selectedConversionDockShips: MutableList<FleetMemberAPI>
        get() = selectedDocks

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(ArgosRetrofitPlugin::class.java, "selectedDocks", "sD")
        }

        const val PICK_DOCKS = "roider_pickConvDocks"
        const val COM_CR = "CR"
    }
}