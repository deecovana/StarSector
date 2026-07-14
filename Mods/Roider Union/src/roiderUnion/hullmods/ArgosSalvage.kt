package roiderUnion.hullmods

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.RepairGantry
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.StringToken

/**
 * Author: SafariJohn
 */
class ArgosSalvage : BaseHullMod() {
    companion object {
        const val ARGOS_SALVAGE_BONUS = 40f
        const val BATTLE_SALVAGE_MULT = 0.2f
        const val MIN_CR = 0.1f
        const val TOKEN_BONUS = "\$bonus"
        const val TOKEN_CR = "\$minCR"
        const val TOKEN_MORE = "\$oneMore"
        const val TOKEN_LESS = "\$oneLess"
        const val TOKEN_TOTAL = "\$total"
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        stats?.dynamic?.getMod(Stats.SALVAGE_VALUE_MULT_MOD)
            ?.modifyFlat(id, ARGOS_SALVAGE_BONUS * 0.01f)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String {
        if (index == 0) return Helper.floatToPercentString(ARGOS_SALVAGE_BONUS)
        return if (index == 1) Helper.multToPercentString(BATTLE_SALVAGE_MULT)
        else ExternalStrings.DEBUG_NULL
    }

    override fun shouldAddDescriptionToTooltip(
        hullSize: HullSize?,
        ship: ShipAPI?, isForModSpec: Boolean
    ): Boolean {
        return true
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: HullSize?, ship: ShipAPI?, width: Float,
        isForModSpec: Boolean
    ) {
        if (Helper.anyNull(tooltip)) return

        val h = Misc.getHighlightColor()
        val bad = Misc.getNegativeHighlightColor()
        tooltip!!.addPara(ExternalStrings.ARGOS_SALVAGE_DESC, Helper.PAD)

        if (Helper.anyNull(ship) || isForModSpec) return

        if (Global.getSettings().currentState == GameState.TITLE) return
        val fleet = Global.getSector().playerFleet
        val fleetMod = RepairGantry.getAdjustedGantryModifier(fleet, null, 0f)
        val currShipMod = ARGOS_SALVAGE_BONUS / 100f
        val fleetModWithOneMore = RepairGantry.getAdjustedGantryModifier(fleet, null, currShipMod)

        var cr = ship!!.currentCR
        for (member in Global.getSector().playerFleet.fleetData.membersListCopy) {
            if (member.id == ship.fleetMemberId) {
                cr = member.repairTracker.cr
            }
        }

        val fleetModTotal = RepairGantry.getAdjustedGantryModifierForPostCombatSalvage(fleet)
        val fleetModWithoutThisShip = RepairGantry.getAdjustedGantryModifier(fleet, ship.fleetMemberId, 0f)

        val tokens = mutableListOf<StringToken>()
        tokens += StringToken(TOKEN_BONUS, Helper.multToPercentString(fleetMod), Misc.getHighlightColor())
        tokens += StringToken(TOKEN_CR, Helper.multToPercentString(MIN_CR), Misc.getNegativeHighlightColor())
        tokens += StringToken(TOKEN_MORE, Helper.multToPercentString(fleetModWithOneMore), Misc.getHighlightColor())
        tokens += StringToken(TOKEN_LESS, Helper.multToPercentString(fleetModWithoutThisShip), Misc.getHighlightColor())
        tokens += StringToken(TOKEN_TOTAL, Helper.multToPercentString(fleetModTotal), Misc.getHighlightColor())

        var text: String
        val hl = if (cr < MIN_CR) {
            text = ExternalStrings.ARGOS_SALVAGE_CR
            Helper.buildHighlightsLists(ExternalStrings.ARGOS_SALVAGE_CR, *tokens.toTypedArray())
        } else {
            if (fleetMod > currShipMod) {
                text = ExternalStrings.ARGOS_SALVAGE_REMOVE
                Helper.buildHighlightsLists(ExternalStrings.ARGOS_SALVAGE_REMOVE, *tokens.toTypedArray())
            } else {
                text = ExternalStrings.ARGOS_SALVAGE_ADD
                Helper.buildHighlightsLists(ExternalStrings.ARGOS_SALVAGE_ADD, *tokens.toTypedArray())
            }
        }
        text = text.replace(TOKEN_BONUS, "%s")
            .replace(TOKEN_CR, "%s")
            .replace(TOKEN_MORE, "%s")
            .replace(TOKEN_LESS, "%s")
            .replace(TOKEN_TOTAL, "%s")
        tooltip.addPara(text, Helper.PAD, hl.second.toTypedArray(), *hl.first.toTypedArray())
    }
}