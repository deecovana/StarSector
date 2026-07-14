package roiderUnion.retrofits.argos

import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.VisualPanelAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import retroLib.Settings
import retroLib.impl.BaseRetrofitPluginView
import retroLib.impl.OptionId
import retroLib.impl.QueueState
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import java.awt.Color
import kotlin.math.absoluteValue

class ArgosRetrofitView(
    private val aModel: ArgosRetrofitPluginModel,
    updateInteractor: () -> Unit
) : BaseRetrofitPluginView(aModel, updateInteractor) {
    companion object {
        const val TOKEN_CR = "\$cr"
        const val TOKEN_MACHINERY = "\$machinery"
        const val TOKEN_SUPPLIES = "\$supplies"
        const val TOKEN_METALS = "\$metals"
        const val OPTION_PICK_DOCKS = "pick_docks"
    }

    override fun updateOptions() {
        val options = model.dialog.optionPanel!!
        options.clearOptions()
        if (model.selectedShips.isEmpty()) {
            setupPickTargetOption(options)
            setupPickSourceOption(options)
            if (model.queueState != QueueState.EMPTY) {
                setupQueueOptions(options)
            }
            setupPickDocksOption(model.dialog.optionPanel!!)
            options.addOption(retroLib.ExternalStrings.RETURN, OptionId.EXIT)
            options.setShortcut(OptionId.EXIT, Keyboard.KEY_ESCAPE, false, false, false, true)
        } else {
            setupConfirmationOptions(options)
        }
    }

    private fun setupPickDocksOption(options: OptionPanelAPI) {
        options.addOption(ExternalStrings.ARGOS_PICK_DOCKS_OPTION, OPTION_PICK_DOCKS)
        if (aModel.availableDockShips.isEmpty()) {
            options.setEnabled(OPTION_PICK_DOCKS, false)
            options.setTooltip(OPTION_PICK_DOCKS, ExternalStrings.ARGOS_PICK_DOCKS_NONE)
        }
    }

    override fun setupConfirmationOptions(options: OptionPanelAPI) {
        if (aModel.isPaidConversion) {
            super.setupConfirmationOptions(options)
            if (aModel.activeDockShips.isEmpty()) {
                disableOptionAllBad(options, OptionId.CONFIRM_RETROFITS, ExternalStrings.ARGOS_NEED_DOCK)
            } else if (aModel.needCR) {
                disableOptionAllBad(options, OptionId.CONFIRM_RETROFITS, ExternalStrings.ARGOS_NEED_CR_THEM)
            }
            return
        }

        options.addOption(retroLib.ExternalStrings.CONFIRM_RETROFITS_OPTION, OptionId.CONFIRM_RETROFITS)

        if (aModel.activeDockShips.isEmpty()) {
            disableOptionAllBad(options, OptionId.CONFIRM_RETROFITS, ExternalStrings.ARGOS_NEED_DOCK)
        } else if (aModel.needCR) {
            disableOptionAllBad(options, OptionId.CONFIRM_RETROFITS, ExternalStrings.ARGOS_NEED_CR)
        } else if (aModel.needRez) {
            disableOptionAllBad(options, OptionId.CONFIRM_RETROFITS, ExternalStrings.ARGOS_NEED_REZ)
        }
        val tCost = aModel.totalRezCost
        val temp = if (tCost > 0) {
            ExternalStrings.ARGOS_PAY_REZ
        } else if (tCost < 0) {
            ExternalStrings.ARGOS_GET_REZ
        } else {
            ExternalStrings.ARGOS_REZ_FREE
        }
        val confirmText = temp.replace(TOKEN_COST, tCost.absoluteValue.toString())
        options.addOptionConfirmation(
            OptionId.CONFIRM_RETROFITS,
            confirmText,
            retroLib.ExternalStrings.CONFIRM_RETROFITS_OPTION_YES,
            retroLib.ExternalStrings.CONFIRM_RETROFITS_OPTION_NO
        )
        options.addOption(retroLib.ExternalStrings.CANCEL_RETROFITS_OPTION, OptionId.CANCEL_RETROFITS)
        options.setShortcut(OptionId.CANCEL_RETROFITS, Keyboard.KEY_ESCAPE, false, false, false, true)
    }

    private fun disableOptionAllBad(options: OptionPanelAPI, option: String, text: String) {
        options.setEnabled(option, false)
        options.setTooltip(option, text)
        options.setTooltipHighlights(option, text)
        options.setTooltipHighlightColors(option, Misc.getNegativeHighlightColor())
    }

    override fun showOptionResult() {
        if (model.optionId == OPTION_PICK_DOCKS) showDockSelectionDialog()
        else super.showOptionResult()
    }

    override fun updateVisual() {
        val visual = model.dialog.visualPanel ?: return
        if (model.currTarget == null) {
            showDocks(visual)
        } else {
            visual.showFleetMemberInfo(model.currTarget)
        }
    }

    private fun showDocks(visual: VisualPanelAPI) {
        val active = Helper.factory?.createEmptyFleet(aModel.factionId, ExternalStrings.ARGOS_ACTIVE, true) ?: return
        for (dock in aModel.activeDockShips) {
            val temp = dock.repairTracker.cr
            active.fleetData.addFleetMember(dock)
            dock.repairTracker.cr = temp
        }
        val avail = Helper.factory?.createEmptyFleet(aModel.factionId, ExternalStrings.ARGOS_AVAILABLE, true) ?: return
        aModel.availableDockShips.filterNot { aModel.activeDockShips.contains(it) }
            .forEach {
                val temp = it.repairTracker.cr
                avail.fleetData.addFleetMember(it)
                it.repairTracker.cr = temp
            }
        visual.showFleetInfo(ExternalStrings.ARGOS_ACTIVE_DOCKS, active, ExternalStrings.ARGOS_AVAILABLE_DOCKS, avail)
    }

    override fun appendLegalCosts(
        builder: StringBuilder,
        highlights: MutableList<String>,
        hlColors: MutableList<Color>,
        cost: Int,
        time: Int
    ) {
        val resources = aModel.argosSourceTextData.first { it.rezCosts[Commodities.CREDITS] == cost }.rezCosts
        if (resources[ArgosRetrofitInteractor.COM_CR] != 0) {
            builder.append(ExternalStrings.ARGOS_CR_COST)
            highlights.add(resources[ArgosRetrofitInteractor.COM_CR]!!.toString())
            hlColors += Misc.getHighlightColor()
        }
        val text = if (aModel.isPaidConversion) {
            super.appendLegalCosts(builder, highlights, hlColors, cost, time)
            return
        } else {
            if (cost > 0) {
                ExternalStrings.ARGOS_RESOURCES_REQ
            } else if (cost < 0) {
                ExternalStrings.ARGOS_RESOURCES_GIVE
            } else {
                retroLib.ExternalStrings.SOURCES_TEXT_LEGAL_FREE
            }
        }
        builder.append(text)

        if (cost != 0) {
            highlights.add(resources[Commodities.HEAVY_MACHINERY]!!.absoluteValue.toString())
            highlights.add(resources[Commodities.SUPPLIES]!!.absoluteValue.toString())
            highlights.add(resources[Commodities.METALS]!!.absoluteValue.toString())
            hlColors += Misc.getHighlightColor()
            hlColors += Misc.getHighlightColor()
            hlColors += Misc.getHighlightColor()
        } else {
            highlights.add(retroLib.ExternalStrings.SOURCES_TEXT_LEGAL_FREE_HL)
            hlColors += Misc.getHighlightColor()
        }
    }

    override fun appendIllegalsCosts(builder: StringBuilder, cost: Int, time: Int) {
        val resources = aModel.argosSourceTextData.first { it.rezCosts[Commodities.CREDITS] == cost }.rezCosts
        if (resources[ArgosRetrofitInteractor.COM_CR] != 0) {
            builder.append(ExternalStrings.ARGOS_CR_COST_ILLEGAL.replace(TOKEN_CR, resources[ArgosRetrofitInteractor.COM_CR]!!.toString()))
        }
        if (aModel.isPaidConversion) {
            super.appendIllegalsCosts(builder, cost, time)
            return
        }
        val text = if (cost > 0) {
            ExternalStrings.ARGOS_RESOURCES_REQ_ILLEGAL
        } else if (cost < 0) {
            ExternalStrings.ARGOS_RESOURCES_GIVE_ILLEGAL
        } else {
            retroLib.ExternalStrings.SOURCES_TEXT_ILLEGAL_FREE
        }
        builder.append(
            text.replace(TOKEN_MACHINERY, resources[Commodities.HEAVY_MACHINERY]!!.absoluteValue.toString())
                .replace(TOKEN_SUPPLIES, resources[Commodities.SUPPLIES]!!.absoluteValue.toString())
                .replace(TOKEN_METALS, resources[Commodities.METALS]!!.absoluteValue.toString())
        )
    }

    override fun displayConfirmationText(text: TextPanelAPI) {
        if (aModel.isPaidConversion) {
            super.displayConfirmationText(text)
            return
        }
        val targetTooltip = text.beginTooltip()
        targetTooltip.addTitle(retroLib.ExternalStrings.CONFIRMATION_TITLE)
        val rows = model.selectedShips.size / COLUMNS + 1
        val iconSize: Float = model.dialog.textWidth / COLUMNS
        targetTooltip.addShipList(COLUMNS, rows, iconSize, model.factionColor, model.selectedShips, 0f)
        text.addTooltip()
        val totalResources = aModel.totalRezCost
        val totalRezString = totalResources.absoluteValue.toString()
        val message = if (model.selectedShips.size > 1) {
            if (totalResources > 0) {
                ExternalStrings.ARGOS_CONFIRM_PAY_MANY.replace(TOKEN_COST, totalRezString)
            } else if (totalResources < 0) {
                ExternalStrings.ARGOS_CONFIRM_GET_MANY.replace(TOKEN_COST, totalRezString)
            } else {
                retroLib.ExternalStrings.CONFIRMATION_FREE_MANY
            }
        } else {
            if (totalResources > 0) {
                ExternalStrings.ARGOS_CONFIRM_PAY.replace(TOKEN_COST, totalRezString)
            } else if (totalResources < 0) {
                ExternalStrings.ARGOS_CONFIRM_GET.replace(TOKEN_COST, totalRezString)
            } else {
                retroLib.ExternalStrings.CONFIRMATION_FREE
            }
        }
        val highlight = ExternalStrings.ARGOS_CONFIRM_HIGHLIGHT.replace(TOKEN_COST, totalRezString)
        text.addPara(message, Misc.getHighlightColor(), highlight)
        val rez = aModel.rezCosts
        Misc.showCost(
            text,
            aModel.factionColor,
            aModel.factionDarkUIColor,
            arrayOf(Commodities.HEAVY_MACHINERY, Commodities.SUPPLIES, Commodities.METALS),
            intArrayOf(
                rez[Commodities.HEAVY_MACHINERY]!!,
                rez[Commodities.SUPPLIES]!!,
                rez[Commodities.METALS]!!
            )
        )
    }

    private fun showDockSelectionDialog() {
        val rows = 1 + model.queued.size / Settings.SHIP_PICK_COLUMNS
        model.dialog.showFleetMemberPickerDialog(
            retroLib.ExternalStrings.SHOW_CANCEL_DIALOG_TITLE,
            retroLib.ExternalStrings.SHOW_CANCEL_DIALOG_YES,
            retroLib.ExternalStrings.SHOW_CANCEL_DIALOG_NO,
            rows,
            Settings.SHIP_PICK_COLUMNS,
            Settings.SHIP_PICK_ICON_SIZE,
            true,
            true,
            aModel.availableDockShips,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    aModel.activeDockShips = members
                    updateInteractor()
                    updateText()
                    updateOptions()
                    updateVisual()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

}