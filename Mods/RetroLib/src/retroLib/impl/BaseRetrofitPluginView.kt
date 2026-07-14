package retroLib.impl

import com.fs.starfarer.api.InteractionDialogImageVisual
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import retroLib.ExternalStrings
import retroLib.Helper
import retroLib.IDs
import retroLib.Settings
import retroLib.api.RetrofitPluginModel
import retroLib.api.RetrofitPluginView
import java.awt.Color
import java.util.*
import kotlin.math.absoluteValue

open class BaseRetrofitPluginView(
    protected val model: RetrofitPluginModel,
    protected val updateInteractor: () -> Unit
) : RetrofitPluginView {
    companion object {
        const val TOKEN_COST = "\$cost"
        const val TOKEN_TIME = "\$time"
        const val TOKEN_SOURCE = "\$source"
        const val TOKEN_TARGET = "\$target"
        const val TOKEN_FIRST_DAYS = "\$firstDays"
        const val TOKEN_ALL_DAYS = "\$allDays"
        const val TOKEN_FACTION_NAME = "\$factionName"
        const val TOKEN_REP_LEVEL = "\$repLevel"
        const val TOKEN_SIZE_NAME = "\$sizeName"
        const val TOKEN_REFUND = "[REFUND]"
        
        const val COLUMNS = 7
    }

    override fun init() {
        updateText()
        updateOptions()
        updateVisual()
    }

    override fun showOptionResult() {
        when (model.optionId) {
            OptionId.PICK_TARGET -> showTargetPickDialog()
            OptionId.PICK_SOURCES -> showSourcesPickDialog()
            OptionId.PRIORITIZE_QUEUED -> showPrioritizeDialog()
            OptionId.CANCEL_QUEUED -> showCancelDialog()
            OptionId.CONFIRM_RETROFITS -> confirmedRetrofits()
            OptionId.CANCEL_RETROFITS -> canceledRetrofits()
            OptionId.EXIT -> clearView()
            else -> {}
        }
    }

    override fun showMouseOverResult() {}

    protected open val notAllowedRetrofitsTitle: String
        get() = ExternalStrings.NOT_ALLOWED_TITLE

    protected open fun updateOptions() {
        val options = model.dialog.optionPanel!!
        options.clearOptions()
        if (model.selectedShips.isEmpty()) {
            setupPickTargetOption(options)
            setupPickSourceOption(options)
            if (model.queueState != QueueState.EMPTY) {
                setupQueueOptions(options)
            }
            options.addOption(ExternalStrings.RETURN, OptionId.EXIT)
            options.setShortcut(OptionId.EXIT, Keyboard.KEY_ESCAPE, false, false, false, true)
        } else {
            setupConfirmationOptions(options)
        }
    }

    protected open fun updateVisual() {
        val visual = model.dialog.visualPanel ?: return
        if (model.currTarget == null) {
            visual.showImageVisual(
                InteractionDialogImageVisual(
                    IDs.ILLUSTRATIONS,
                    IDs.CONSTRUCTION,
                    640f, // extern?
                    400f)
            )
        } else {
            visual.showFleetMemberInfo(model.currTarget)
        }
    }

    protected open fun updateText() {
        val text = model.dialog.textPanel ?: return
        text.clear()
        if (model.selectedShips.isNotEmpty()) {
            displayConfirmationText(text)
            return
        }
        if (model.queued.isNotEmpty()) displayQueueText(text)
        if (model.currTarget == null) displayTargetsText(text) else displaySourcesText(text)
    }

    protected open fun confirmedRetrofits() {
        updateText()
        updateOptions()
    }

    protected open fun canceledRetrofits() {
        updateText()
        updateOptions()
    }

    protected open fun displayConfirmationText(text: TextPanelAPI) {
        val targetTooltip = text.beginTooltip()
        targetTooltip.addTitle(ExternalStrings.CONFIRMATION_TITLE)
        val rows = model.selectedShips.size / COLUMNS + 1
        val iconSize: Float = model.dialog.textWidth / COLUMNS
        targetTooltip.addShipList(COLUMNS, rows, iconSize, model.factionColor, model.selectedShips, 0f)
        text.addTooltip()
        val cost = model.confirmationCost
        val message = if (model.selectedShips.size > 1) {
            if (cost > 0) {
                ExternalStrings.CONFIRMATION_PAY_MANY.replace(TOKEN_COST, Misc.getDGSCredits(cost.absoluteValue))
            } else if (cost < 0) {
                ExternalStrings.CONFIRMATION_GET_MANY.replace(TOKEN_COST, Misc.getDGSCredits(cost.absoluteValue))
            } else {
                ExternalStrings.CONFIRMATION_FREE_MANY
            }
        } else {
            if (cost > 0) {
                ExternalStrings.CONFIRMATION_PAY.replace(TOKEN_COST, Misc.getDGSCredits(cost.absoluteValue))
            } else if (cost < 0) {
                ExternalStrings.CONFIRMATION_GET.replace(TOKEN_COST, Misc.getDGSCredits(cost.absoluteValue))
            } else {
                ExternalStrings.CONFIRMATION_FREE
            }
        }
        if (model.sModRefundXP > 0) {
            text.addPara(ExternalStrings.SMOD_REFUND
                .replace(TOKEN_REFUND, Helper.multToPercentString(model.sModRefundXP)),
                Misc.getStoryBrightColor())
        }
        text.addPara(message, Misc.getHighlightColor(), Misc.getDGSCredits(cost.absoluteValue))
    }

    protected open fun displayQueueText(text: TextPanelAPI) {
        val temp = if (model.firstDaysRemaining == 1 && model.queuedDays == 1) {
            ExternalStrings.QUEUE_DISPLAY_TEXT_ONE_ONE
        } else if (model.firstDaysRemaining == 1) {
            ExternalStrings.QUEUE_DISPLAY_TEXT_ONE_MANY
        } else if (model.queuedDays == 1) {
            ExternalStrings.QUEUE_DISPLAY_TEXT_MANY_ONE
        } else {
            ExternalStrings.QUEUE_DISPLAY_TEXT_MANY_MANY
        }
        val message = temp.replace(TOKEN_SOURCE, model.firstSourceName)
            .replace(TOKEN_TARGET, model.firstTargetName)
            .replace(TOKEN_FIRST_DAYS, model.firstDaysRemaining.toString())
            .replace(TOKEN_ALL_DAYS, model.queuedDays.toString())
        text.addPara(message, Misc.getHighlightColor())
        val tootip = text.beginTooltip()
        tootip.addTitle(ExternalStrings.QUEUE_DISPLAY_TEXT_TITLE)
        val rows = 1 + (model.queuedMembers.size - 1) / COLUMNS
        val iconSize = model.dialog.textWidth / COLUMNS
        tootip.addShipList(
            COLUMNS,
            rows,
            iconSize,
            model.factionColor,
            model.queuedMembers,
            0f
        )
        text.addTooltip()
    }

    protected open fun displayTargetsText(text: TextPanelAPI) {
        text.addPara(ExternalStrings.TARGETS_TEXT_INSTRUCTIONS, Misc.getButtonTextColor())
        if (model.targetsAvailable.isNotEmpty()) showCatalog(
            ExternalStrings.TARGETS_TEXT_AVAILABLE_TITLE,
            model.targetsAvailable,
            model.factionColor
        )
        if (model.targetsUnavailable.isNotEmpty()) showCatalog(
            ExternalStrings.TARGETS_TEXT_UNAVAILABLE_TITLE,
            model.targetsUnavailable,
            Misc.getGrayColor()
        )
        if (model.targetsIllegal.isNotEmpty()) showCatalog(
            ExternalStrings.TARGETS_TEXT_ILLEGAL_TITLE,
            model.targetsIllegal,
            Misc.getNegativeHighlightColor()
        )
        if (model.targetsNotAllowed.isNotEmpty()) showCatalog(
            notAllowedRetrofitsTitle,
            model.targetsNotAllowed,
            Misc.getNegativeHighlightColor()
        )
    }

    protected open fun displaySourcesText(text: TextPanelAPI) {
        val target = ExternalStrings.SOURCES_TEXT_TARGET.replace(TOKEN_TARGET, model.currTarget!!.variant.fullDesignationWithHullName)
        text.addPara(target, Misc.getButtonTextColor())
        val builder = StringBuilder()
        val highlights: MutableList<String> = ArrayList()
        val hlColors: MutableList<Color> = ArrayList()
        val illegalCosts = StringBuilder()
        val sourceData = model.sourceTextData
        var first = true
        for (data in sourceData.filter { it.isAllowed && it.isLegal }) {
            if (first) first = false else builder.append("\n")
            builder.append(data.sourceName).append("\n")
            appendFrameReturnText(builder, highlights, hlColors, data.frameReturnSize)
            appendLegalCosts(builder, highlights, hlColors, data.cost, data.time)
        }
        first = true
        for (data in sourceData.filter { !it.isAllowed || !it.isLegal }) {
            if (first) first = false else illegalCosts.append("\n")
            illegalCosts.append(data.sourceName).append("\n")
            appendIllegalFrameReturnText(illegalCosts, data.frameReturnSize)
            appendIllegalsCosts(illegalCosts, data.cost, data.time)
            if (!data.isAllowed) {
                if (data.spec != null) illegalCosts.append(getNotAllowedRetrofitText(data.spec))
                else illegalCosts.append(getNotAllowedRetrofitText(data.wingSpec!!))
            }
            else illegalCosts.append(getIllegalRetrofitText(!data.isLegalRep, data.repName, !data.isLegalCom))
        }
        if (builder.isNotEmpty()) {
            var legal = builder.toString()
            highlights.forEach {
                legal = legal.replaceFirst("%s", it)
            }
            text.addPara(legal)
            text.highlightInLastPara(*highlights.toTypedArray())
            text.setHighlightColorsInLastPara(*hlColors.toTypedArray())
        }
        if (illegalCosts.isNotEmpty()) text.addPara(
            illegalCosts.toString(),
            Misc.getNegativeHighlightColor(), illegalCosts.toString()
        )
        if (builder.isEmpty() && illegalCosts.isEmpty()) {
            text.addPara(ExternalStrings.SOURCES_TEXT_NONE)
        }
    }

    protected fun appendFrameReturnText(
        builder: StringBuilder,
        highlights: MutableList<String>,
        hlColors: MutableList<Color>,
        frameReturnSize: HullSize?
    ) {
        if (frameReturnSize == null) return
        builder.append(ExternalStrings.SOURCES_TEXT_FRAME_RETURN)
        val hl = ExternalStrings.SOURCES_TEXT_FRAME_RETURN_HL.replace(TOKEN_SIZE_NAME, frameReturnSize.name.lowercase())
        highlights += hl
        hlColors += Misc.getPositiveHighlightColor()
    }

    protected fun appendIllegalFrameReturnText(
        builder: StringBuilder,
        frameReturnSize: HullSize?
    ) {
        if (frameReturnSize == null) return
        builder.append(ExternalStrings.SOURCES_TEXT_FRAME_RETURN_ILLEGAL.replace(TOKEN_SIZE_NAME, frameReturnSize.name.lowercase()))
    }

    protected fun showCatalog(name: String, members: List<FleetMemberAPI>, color: Color?) {
        members.forEach { Helper.prepDisplayShip(it, true) }
        val tooltip = model.dialog.textPanel.beginTooltip()
        tooltip.addTitle(name)
        val rows = 1 + (members.size - 1) / COLUMNS
        val iconSize: Float = model.dialog.textWidth / COLUMNS
        tooltip.addShipList(COLUMNS, rows, iconSize, color, members, 0f)
        model.dialog.textPanel.addTooltip()
    }

    protected open fun appendIllegalsCosts(builder: StringBuilder, cost: Int, time: Int) {
        val text = if (cost > 0) {
            when (time) {
                0 -> ExternalStrings.SOURCES_TEXT_ILLEGAL_PAY
                1 -> ExternalStrings.SOURCES_TEXT_ILLEGAL_PAY_TIME_ONE
                else -> ExternalStrings.SOURCES_TEXT_ILLEGAL_PAY_TIME
            }
        } else if (cost < 0) {
            when (time) {
                0 -> ExternalStrings.SOURCES_TEXT_ILLEGAL_GET
                1 -> ExternalStrings.SOURCES_TEXT_ILLEGAL_GET_TIME_ONE
                else -> ExternalStrings.SOURCES_TEXT_ILLEGAL_GET_TIME
            }
        } else {
            when (time) {
                0 -> ExternalStrings.SOURCES_TEXT_ILLEGAL_FREE
                1 -> ExternalStrings.SOURCES_TEXT_ILLEGAL_TIME_ONE
                else -> ExternalStrings.SOURCES_TEXT_ILLEGAL_TIME
            }
        }
        builder.append(text.replace(TOKEN_COST, Misc.getDGSCredits(cost.absoluteValue.toFloat())).replace(TOKEN_TIME, time.toString()))
    }

    protected fun getIllegalRetrofitText(isLowRep: Boolean, repLevelName: String, needCommission: Boolean): String? {
        val result = if (isLowRep && needCommission) {
            ExternalStrings.SOURCES_TEXT_ILLEGAL_REP_COM
        } else if (isLowRep) {
            ExternalStrings.SOURCES_TEXT_ILLEGAL_REP
        } else if (needCommission) {
            ExternalStrings.SOURCES_TEXT_ILLEGAL_COM
        } else {
            null
        }
        return result?.replace(TOKEN_FACTION_NAME, model.factionName)
            ?.replace(TOKEN_REP_LEVEL, repLevelName.lowercase())
    }

    protected open fun getNotAllowedRetrofitText(spec: ShipHullSpecAPI): String = ""

    protected open fun getNotAllowedRetrofitText(spec: FighterWingSpecAPI): String = ""

    protected open fun appendLegalCosts(
        builder: StringBuilder,
        highlights: MutableList<String>,
        hlColors: MutableList<Color>,
        cost: Int,
        time: Int
    ) {
        val text = if (cost > 0) {
            when (time) {
                0 -> ExternalStrings.SOURCES_TEXT_LEGAL_PAY
                1 -> ExternalStrings.SOURCES_TEXT_LEGAL_PAY_TIME_ONE
                else -> ExternalStrings.SOURCES_TEXT_LEGAL_PAY_TIME
            }
        } else if (cost < 0) {
            when (time) {
                0 -> ExternalStrings.SOURCES_TEXT_LEGAL_GET
                1 -> ExternalStrings.SOURCES_TEXT_LEGAL_GET_TIME_ONE
                else -> ExternalStrings.SOURCES_TEXT_LEGAL_GET_TIME
            }
        } else {
            when (time) {
                0 -> ExternalStrings.SOURCES_TEXT_LEGAL_FREE
                1 -> ExternalStrings.SOURCES_TEXT_LEGAL_TIME_ONE
                else -> ExternalStrings.SOURCES_TEXT_LEGAL_TIME
            }
        }
        builder.append(text)

        if (cost != 0) {
            highlights.add(Misc.getDGSCredits(cost.absoluteValue.toFloat()))
            hlColors += Misc.getHighlightColor()
        }
        if (time != 0) {
            highlights.add(time.toString())
            hlColors += Misc.getHighlightColor()
        }
        if (cost == 0 && time == 0) {
            highlights.add(ExternalStrings.SOURCES_TEXT_LEGAL_FREE_HL)
            hlColors += Misc.getHighlightColor()
        }
    }

    protected fun setupPickTargetOption(options: OptionPanelAPI) {
        options.addOption(ExternalStrings.PICK_TARGET_OPTION, OptionId.PICK_TARGET)
        if (model.targets.isEmpty()) {
            options.setEnabled(OptionId.PICK_TARGET, false)
            options.setTooltip(OptionId.PICK_TARGET, ExternalStrings.PICK_TARGET_OPTION_NONE)
        }
    }

    protected open fun setupPickSourceOption(options: OptionPanelAPI) {
        val queue = if (model.queueState == QueueState.EMPTY) {
            ExternalStrings.PICK_SOURCE_OPTION
        } else {
            ExternalStrings.PICK_SOURCE_OPTION_QUEUE
        }
        val pickSourcesId = OptionId.PICK_SOURCES
        options.addOption(queue, pickSourcesId)
        when (model.pickSourcesState) {
            PickSourcesOptionState.NO_TARGET -> {
                options.setTooltip(pickSourcesId, ExternalStrings.PICK_SOURCE_OPTION_TOOLTIP)
                options.setEnabled(pickSourcesId, false)
            }
            PickSourcesOptionState.NONE_AVAILABLE -> {
                val targetName = model.currTarget?.hullSpec?.hullNameWithDashClass ?: ExternalStrings.DEBUG_NULL
                options.setTooltip(
                    pickSourcesId,
                    ExternalStrings.PICK_SOURCE_OPTION_NONE.replace(TOKEN_TARGET, targetName)
                )
                options.setEnabled(pickSourcesId, false)
            }
            PickSourcesOptionState.NOT_ALLOWED -> {
                setNotAllowedTooltip(options, pickSourcesId)
                options.setEnabled(pickSourcesId, false)
            }
            PickSourcesOptionState.ILLEGAL -> {
                setIllegalTooltip(options, pickSourcesId)
                options.setEnabled(pickSourcesId, false)
            }
            else -> {}
        }
        if (model.currTarget == null) {
            options.setTooltip(pickSourcesId, ExternalStrings.PICK_SOURCE_OPTION_TOOLTIP)
            options.setEnabled(pickSourcesId, false)
        } else {
            if (model.availableSources.isEmpty()) {
                val targetName = model.currTarget?.hullSpec?.hullNameWithDashClass ?: ExternalStrings.DEBUG_NULL
                options.setTooltip(
                    pickSourcesId,
                    ExternalStrings.PICK_SOURCE_OPTION_NONE.replace(TOKEN_TARGET, targetName)
                )
                options.setEnabled(pickSourcesId, false)
            }
        }
    }

    protected fun setupQueueOptions(options: OptionPanelAPI) {
        options.addOption(ExternalStrings.PRIORITIZE_QUEUED_OPTION, OptionId.PRIORITIZE_QUEUED)
        options.addOption(ExternalStrings.CANCEL_QUEUED_OPTION, OptionId.CANCEL_QUEUED)
        if (model.queueState == QueueState.ONE) {
            options.setEnabled(OptionId.PRIORITIZE_QUEUED, false)
        }
    }

    protected open fun setupConfirmationOptions(options: OptionPanelAPI) {
        options.addOption(ExternalStrings.CONFIRM_RETROFITS_OPTION, OptionId.CONFIRM_RETROFITS)
        val tCost = model.confirmationCost
        if (tCost > (Helper.sector?.playerFleet?.cargo?.credits?.get() ?: 0f)) {
            options.setEnabled(OptionId.CONFIRM_RETROFITS, false)
            options.setTooltip(OptionId.CONFIRM_RETROFITS, ExternalStrings.CONFIRM_RETROFITS_OPTION_CREDITS)
            options.setTooltipHighlights(OptionId.CONFIRM_RETROFITS, ExternalStrings.CONFIRM_RETROFITS_OPTION_CREDITS)
            options.setTooltipHighlightColors(OptionId.CONFIRM_RETROFITS, Misc.getNegativeHighlightColor())
        }
        val temp = if (tCost > 0) {
            ExternalStrings.CONFIRM_RETROFITS_OPTION_PAY
        } else if (tCost < 0) {
            ExternalStrings.CONFIRM_RETROFITS_OPTION_GET
        } else {
            ExternalStrings.CONFIRM_RETROFITS_OPTION_FREE
        }
        val confirmText = temp.replace(TOKEN_COST, Misc.getDGSCredits(tCost.absoluteValue))
        options.addOptionConfirmation(
            OptionId.CONFIRM_RETROFITS,
            confirmText,
            ExternalStrings.CONFIRM_RETROFITS_OPTION_YES,
            ExternalStrings.CONFIRM_RETROFITS_OPTION_NO
        )
        options.addOption(ExternalStrings.CANCEL_RETROFITS_OPTION, OptionId.CANCEL_RETROFITS)
        options.setShortcut(OptionId.CANCEL_RETROFITS, Keyboard.KEY_ESCAPE, false, false, false, true)
    }

    protected open fun setIllegalTooltip(options: OptionPanelAPI, option: String) {
        val text = if (model.isLowRep && model.needCommission) {
            ExternalStrings.PICK_SOURCE_OPTION_REP_COM
        } else if (model.isLowRep) {
            ExternalStrings.PICK_SOURCE_OPTION_REP
        } else if (model.needCommission) {
            ExternalStrings.PICK_SOURCE_OPTION_COM
        } else {
            ""
        }
        options.setTooltip(option, text)
        options.setTooltipHighlights(option, text)
        options.setTooltipHighlightColors(option, Misc.getNegativeHighlightColor())
    }

    protected open fun setNotAllowedTooltip(options: OptionPanelAPI, option: String) {}

    protected open fun showSourcesPickDialog() {
        val rows = 1 + model.availableSources.size / Settings.SHIP_PICK_COLUMNS
        model.dialog.showFleetMemberPickerDialog(
            ExternalStrings.SHOW_SOURCES_DIALOG_TITLE,
            ExternalStrings.SHOW_SOURCES_DIALOG_YES,
            ExternalStrings.SHOW_SOURCES_DIALOG_NO,
            rows,
            Settings.SHIP_PICK_COLUMNS,
            Settings.SHIP_PICK_ICON_SIZE,
            true,
            true,
            model.availableSources,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    if (members.isEmpty()) return
                    model.selectedShips = members
                    updateInteractor()
                    updateOptions()
                    updateText()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    protected open fun clearView() {
        model.dialog.textPanel.clear()
        model.dialog.optionPanel.clearOptions()
        model.dialog.visualPanel.fadeVisualOut()
        if (Helper.soundPlayer?.currentMusicId != null) {
            Helper.soundPlayer?.setSuspendDefaultMusicPlayback(false)
            Helper.soundPlayer?.restartCurrentMusic()
        }
    }

    protected open fun showTargetPickDialog() {
        val fleet = Helper.factory?.createEmptyFleet(Factions.NEUTRAL, "", true)
        model.targets.filterNot { it.isFighterWing }.forEach {
            fleet?.fleetData?.addFleetMember(it)
            Helper.prepDisplayShip(it, true)
        }
        val rows = 1 + model.targets.size / Settings.SHIP_PICK_COLUMNS
        model.dialog.showFleetMemberPickerDialog(
            ExternalStrings.SHOW_TARGET_DIALOG_TITLE,
            ExternalStrings.SHOW_TARGET_DIALOG_YES,
            ExternalStrings.SHOW_TARGET_DIALOG_NO,
            rows,
            Settings.SHIP_PICK_COLUMNS,
            Settings.SHIP_PICK_ICON_SIZE,
            true,
            false,
            model.targets,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    model.currTarget = members.getOrNull(0)
                    updateInteractor()
                    updateText()
                    updateOptions()
                    updateVisual()
                }

                override fun cancelledFleetMemberPicking() {}
            })
        fleet?.membersWithFightersCopy?.asReversed()?.forEach {
            fleet.fleetData?.removeFleetMember(it)
        }
    }

    protected open fun showPrioritizeDialog() {
        val rows = 1 + model.queued.size / Settings.SHIP_PICK_COLUMNS
        model.dialog.showFleetMemberPickerDialog(
            ExternalStrings.SHOW_PRIORITIZE_DIALOG_TITLE,
            ExternalStrings.SHOW_PRIORITIZE_DIALOG_YES,
            ExternalStrings.SHOW_PRIORITIZE_DIALOG_NO,
            rows,
            Settings.SHIP_PICK_COLUMNS,
            Settings.SHIP_PICK_ICON_SIZE,
            true,
            true,
            model.queued.map { it.source },
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    model.selectedShips = members
                    updateInteractor()
                    updateText()
                    updateOptions()
                    updateVisual()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    protected open fun showCancelDialog() {
        val rows = 1 + model.queued.size / Settings.SHIP_PICK_COLUMNS
        model.dialog.showFleetMemberPickerDialog(
            ExternalStrings.SHOW_CANCEL_DIALOG_TITLE,
            ExternalStrings.SHOW_CANCEL_DIALOG_YES,
            ExternalStrings.SHOW_CANCEL_DIALOG_NO,
            rows,
            Settings.SHIP_PICK_COLUMNS,
            Settings.SHIP_PICK_ICON_SIZE,
            true,
            true,
            model.queued.map { it.source },
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    model.selectedShips = members
                    updateInteractor()
                    updateText()
                    updateOptions()
                    updateVisual()
                }

                override fun cancelledFleetMemberPicking() {}
            })
    }

    fun showRetrofittingMessages(cost: Double, stripped: Boolean) {
        val text = if (cost > 0) {
            ExternalStrings.MESSAGE_PAID
        } else if (cost < 0) {
            ExternalStrings.MESSAGE_RECEIVED
        } else {
            null
        }
        if (text != null) {
            Helper.sector?.campaignUI?.messageDisplay?.addMessage(text.replace(TOKEN_COST, Misc.getDGSCredits(cost.absoluteValue.toFloat())))
        }
        if (stripped) Helper.sector?.campaignUI?.messageDisplay?.addMessage(ExternalStrings.MESSAGE_STRIPPED)
    }
}