package retroLib

import org.json.JSONObject
import org.magiclib.kotlin.map
import java.lang.Exception

object ExternalStrings {
    const val DEBUG_NULL = "NULL"

    private const val STRINGS = "data/strings/strings.json"

    private const val ID = "RetroLib"
    private fun get(id: String): String {
        return Helper.settings?.getString(ID, id) ?: "N/A $id"
    }

    private fun getList(id: String): List<String> {
        val result = HashSet<String>()
        try {
            val json = Helper.settings?.loadJSON(STRINGS, Helper.modId)
            val array = json?.getJSONObject(ID)?.getJSONArray(id) ?: throw NullPointerException()
            result.addAll(array.map { it: JSONObject -> it.toString() })
        } catch (ex: Exception) {
            return listOf(DEBUG_NULL)
        }
        return result.toList()
    }

    val NUMBER_PERCENT = get("numberPercent")
    val NOT_ALLOWED_TITLE = get("notAllowedTitle")
    val RETURN = get("return")
    val CONFIRMATION_TITLE = get("confirmationTitle")
    val CONFIRMATION_FREE = get("confirmationFree")
    val CONFIRMATION_PAY = get("confirmationPay")
    val CONFIRMATION_GET = get("confirmationGet")
    val CONFIRMATION_FREE_MANY = get("confirmationFreeMany")
    val CONFIRMATION_PAY_MANY = get("confirmationPayMany")
    val CONFIRMATION_GET_MANY = get("confirmationGetMany")
    val QUEUE_DISPLAY_TEXT_ONE_ONE = get("queueDisplayTextOneOne")
    val QUEUE_DISPLAY_TEXT_ONE_MANY = get("queueDisplayTextOneMany")
    val QUEUE_DISPLAY_TEXT_MANY_ONE = get("queueDisplayTextManyOne")
    val QUEUE_DISPLAY_TEXT_MANY_MANY = get("queueDisplayTextManyMany")
    val QUEUE_DISPLAY_TEXT_TITLE = get("queueDisplayTextTitle")
    val TARGETS_TEXT_INSTRUCTIONS = get("targetsTextInstructions")
    val TARGETS_TEXT_AVAILABLE_TITLE = get("targetsTextAvailableTitle")
    val TARGETS_TEXT_UNAVAILABLE_TITLE = get("targetsTextUnavailableTitle")
    val TARGETS_TEXT_ILLEGAL_TITLE = get("targetsTextIllegalTitle")
    val SOURCES_TEXT_TARGET = get("sourcesTextTarget")
    val SOURCES_TEXT_NONE = get("sourcesTextNone")
    val SOURCES_TEXT_ILLEGAL_PAY_TIME = get("sourcesTextIllegalPayTime")
    val SOURCES_TEXT_ILLEGAL_GET_TIME = get("sourcesTextIllegalGetTime")
    val SOURCES_TEXT_ILLEGAL_PAY_TIME_ONE = get("sourcesTextIllegalPayTimeOne")
    val SOURCES_TEXT_ILLEGAL_GET_TIME_ONE = get("sourcesTextIllegalGetTimeOne")
    val SOURCES_TEXT_ILLEGAL_PAY = get("sourcesTextIllegalPay")
    val SOURCES_TEXT_ILLEGAL_GET = get("sourcesTextIllegalGet")
    val SOURCES_TEXT_ILLEGAL_TIME = get("sourcesTextIllegalTime")
    val SOURCES_TEXT_ILLEGAL_TIME_ONE = get("sourcesTextIllegalTimeOne")
    val SOURCES_TEXT_ILLEGAL_FREE = get("sourcesTextIllegalFree")
    val SOURCES_TEXT_ILLEGAL_REP_COM = get("sourcesTextIllegalRepCom")
    val SOURCES_TEXT_ILLEGAL_REP = get("sourcesTextIllegalRep")
    val SOURCES_TEXT_ILLEGAL_COM = get("sourcesTextIllegalCom")
    val SOURCES_TEXT_LEGAL_PAY_TIME = get("sourcesTextLegalPayTime")
    val SOURCES_TEXT_LEGAL_GET_TIME = get("sourcesTextLegalGetTime")
    val SOURCES_TEXT_LEGAL_PAY_TIME_ONE = get("sourcesTextLegalPayTimeOne")
    val SOURCES_TEXT_LEGAL_GET_TIME_ONE = get("sourcesTextLegalGetTimeOne")
    val SOURCES_TEXT_LEGAL_PAY = get("sourcesTextLegalPay")
    val SOURCES_TEXT_LEGAL_GET = get("sourcesTextLegalGet")
    val SOURCES_TEXT_LEGAL_TIME = get("sourcesTextLegalTime")
    val SOURCES_TEXT_LEGAL_TIME_ONE = get("sourcesTextLegalTimeOne")
    val SOURCES_TEXT_LEGAL_FREE = get("sourcesTextLegalFree")
    val SOURCES_TEXT_LEGAL_FREE_HL = get("sourcesTextLegalFreeHL")
    val SOURCES_TEXT_FRAME_RETURN = get("sourcesTextFrameReturn")
    val SOURCES_TEXT_FRAME_RETURN_HL = get("sourcesTextFrameReturnHL")
    val SOURCES_TEXT_FRAME_RETURN_ILLEGAL = get("sourcesTextFrameReturnIllegal")
    val PICK_TARGET_OPTION = get("pickTargetOption")
    val PICK_TARGET_OPTION_NONE = get("pickTargetOptionNone")
    val PICK_SOURCE_OPTION = get("pickSourceOption")
    val PICK_SOURCE_OPTION_QUEUE = get("pickSourceOptionQueue")
    val PICK_SOURCE_OPTION_TOOLTIP = get("pickSourceOptionTooltip")
    val PICK_SOURCE_OPTION_NONE = get("pickSourceOptionNone")
    val PICK_SOURCE_OPTION_REP_COM = get("pickSourceOptionRepCom")
    val PICK_SOURCE_OPTION_REP = get("pickSourceOptionRep")
    val PICK_SOURCE_OPTION_COM = get("pickSourceOptionCom")
    val PRIORITIZE_QUEUED_OPTION = get("prioritizeQueuedOption")
    val CANCEL_QUEUED_OPTION = get("cancelQueuedOption")
    val CONFIRM_RETROFITS_OPTION = get("confirmRetrofitsOption")
    val CONFIRM_RETROFITS_OPTION_CREDITS = get("confirmRetrofitsOptionCredits")
    val CONFIRM_RETROFITS_OPTION_PAY = get("confirmRetrofitsOptionPay")
    val CONFIRM_RETROFITS_OPTION_GET = get("confirmRetrofitsOptionGet")
    val CONFIRM_RETROFITS_OPTION_FREE = get("confirmRetrofitsOptionFree")
    val CONFIRM_RETROFITS_OPTION_YES = get("confirmRetrofitsOptionYes")
    val CONFIRM_RETROFITS_OPTION_NO = get("confirmRetrofitsOptionNo")
    val CANCEL_RETROFITS_OPTION = get("cancelRetrofitsOption")
    val SHOW_SOURCES_DIALOG_TITLE = get("showSourcesDialogTitle")
    val SHOW_SOURCES_DIALOG_YES = get("showSourcesDialogYes")
    val SHOW_SOURCES_DIALOG_NO = get("showSourcesDialogNo")
    val SHOW_TARGET_DIALOG_TITLE = get("showTargetDialogTitle")
    val SHOW_TARGET_DIALOG_YES = get("showTargetDialogYes")
    val SHOW_TARGET_DIALOG_NO = get("showTargetDialogNo")
    val SHOW_PRIORITIZE_DIALOG_TITLE = get("showPrioritizeDialogTitle")
    val SHOW_PRIORITIZE_DIALOG_YES = get("showPrioritizeDialogYes")
    val SHOW_PRIORITIZE_DIALOG_NO = get("showPrioritizeDialogNo")
    val SHOW_CANCEL_DIALOG_TITLE = get("showCancelDialogTitle")
    val SHOW_CANCEL_DIALOG_YES = get("showCancelDialogYes")
    val SHOW_CANCEL_DIALOG_NO = get("showCancelDialogNo")
    val MESSAGE_PAID = get("messagePaid")
    val MESSAGE_RECEIVED = get("messageReceived")
    val MESSAGE_STRIPPED = get("messageStripped")
    val MESSAGE_BONUS_XP = get("messageBonusXP")
    val SMOD_REFUND = get("sModRefund")
}