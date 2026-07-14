package roiderUnion.submarkets

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.util.Highlights
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import java.util.*

class UnionHQSubmarketView(private val model: UnionHQSubmarketModel) {

    companion object {
        val MIN_STANDING = RepLevel.FAVORABLE
        const val TOKEN_FACTION = "\$factionName"
        const val TOKEN_REQ_NAME = "\$reqName"
        const val TOKEN_STACK_NAME = "\$stackName"
    }

    fun getName(): String {
        return Helper.roiders?.displayName ?: ExternalStrings.DEBUG_NULL
    }

    fun getIllegalTransferText(stack: CargoStackAPI): String {
        return if (model.reqName != null) {
            if (model.commissionRequired) {
                ExternalStrings.SUBMARKET_HQ_TRANSFER_COM_REQ
                    .replace(TOKEN_FACTION, model.factionName)
                    .replace(TOKEN_REQ_NAME, model.reqName!!.lowercase(Locale.getDefault()))
            } else {
                ExternalStrings.SUBMARKET_HQ_TRANSFER_REP_REQ
                    .replace(TOKEN_FACTION, model.factionName)
                    .replace(TOKEN_REQ_NAME, model.reqName!!.lowercase(Locale.getDefault()))
            }
        } else ExternalStrings.SUBMARKET_HQ_TRANSFER_ILLEGAL
            .replace(TOKEN_STACK_NAME, stack.displayName)
    }

    fun getDefaultIllegalTransferText(): String {
        return ExternalStrings.SUBMARKET_HQ_TRANSFER_ILLEGAL_DEFAULT
    }

    fun getIllegalTransferTextHighlights(): Highlights {
        val result = Highlights()
        val hl1 = ExternalStrings.SUBMARKET_HQ_TRANSFER_HL1
            .replace(TOKEN_FACTION, model.factionName)
            .replace(TOKEN_REQ_NAME, model.reqName?.lowercase(Locale.getDefault()) ?: ExternalStrings.DEBUG_NULL)
        val hl2 = ExternalStrings.SUBMARKET_HQ_TRANSFER_HL2
            .replace(TOKEN_FACTION, model.factionName)
        result.append(hl1, Misc.getNegativeHighlightColor())
        if (model.commissionRequired) result.append(hl2, Misc.getNegativeHighlightColor())
        return result
    }

    fun getIllegalShipTransferText(): String {
        return if (model.reqName != null && model.commissionRequired) {
            ExternalStrings.SUBMARKET_HQ_TRANSFER_SHIP_COM_REP_REQ
                .replace(TOKEN_FACTION, model.factionName)
                .replace(TOKEN_REQ_NAME, model.reqName?.lowercase(Locale.getDefault()) ?: ExternalStrings.DEBUG_NULL)
        } else if (model.reqName != null) {
            ExternalStrings.SUBMARKET_HQ_TRANSFER_SHIP_REP_REQ
                .replace(TOKEN_FACTION, model.factionName)
                .replace(TOKEN_REQ_NAME, model.reqName?.lowercase(Locale.getDefault()) ?: ExternalStrings.DEBUG_NULL)
        } else if (model.commissionRequired) {
            ExternalStrings.SUBMARKET_HQ_TRANSFER_SHIP_COM_REQ
                .replace(TOKEN_FACTION, model.factionName)
        } else {
            ExternalStrings.SUBMARKET_ILLEGAL_SELL
        }
    }

    fun getIllegalShipTransferTextHighlights(): Highlights {
        val result = Highlights()
        val text = if (model.reqName != null && model.commissionRequired) {
            ExternalStrings.SUBMARKET_HQ_TRANSFER_SHIP_COM_REP_REQ
                .replace(TOKEN_FACTION, model.factionName)
                .replace(TOKEN_REQ_NAME, model.reqName?.lowercase(Locale.getDefault()) ?: ExternalStrings.DEBUG_NULL)
        } else if (model.reqName != null) {
            ExternalStrings.SUBMARKET_HQ_TRANSFER_SHIP_REP_REQ
                .replace(TOKEN_FACTION, model.factionName)
                .replace(TOKEN_REQ_NAME, model.reqName?.lowercase(Locale.getDefault()) ?: ExternalStrings.DEBUG_NULL)
        } else if (model.commissionRequired) {
            ExternalStrings.SUBMARKET_HQ_TRANSFER_SHIP_COM_REQ
                .replace(TOKEN_FACTION, model.factionName)
        } else {
            ExternalStrings.SUBMARKET_ILLEGAL_SELL
        }
        result.append(text, Misc.getNegativeHighlightColor())
        return result
    }

    fun getTooltipAppendix() : String? {
        return when (model.tooltipAppendix) {
            UnionHQTooltipAppendix.NOT_FUNCTIONAL -> ExternalStrings.SUBMARKET_HQ_TOOLTIP_NON_FUNCTIONAL
            UnionHQTooltipAppendix.LOW_REP -> ExternalStrings.SUBMARKET_HQ_TOOLTIP_LOW_REP
                .replace(TOKEN_FACTION, model.factionName)
                .replace(TOKEN_REQ_NAME, MIN_STANDING.displayName.lowercase(Locale.getDefault()))
            UnionHQTooltipAppendix.SNEAK -> ExternalStrings.SUBMARKET_HQ_TOOLTIP_SNEAK
            UnionHQTooltipAppendix.OTHER -> null
        }
    }
}
