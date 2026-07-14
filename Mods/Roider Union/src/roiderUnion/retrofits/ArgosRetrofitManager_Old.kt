package roiderUnion.retrofits

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.ids.Fitters
import roiderUnion.retrofits.old.base.BaseRetrofitManager
import roiderUnion.retrofits.old.base.RetrofitData
import roiderUnion.retrofits.old.base.RetrofitsKeeper

/**
 * Author: SafariJohn
 */
class ArgosRetrofitManager_Old(
    entity: SectorEntityToken?,
    faction: FactionAPI, private val offerings: List<String>?
) : BaseRetrofitManager(Fitters.ARGOS, entity, faction) {

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(ArgosRetrofitManager_Old::class.java, "offerings", "o")
        }
    }

    override fun verifyData(
        id: String?,
        fitter: String?,
        source: String?,
        target: String?,
        cost: Double,
        time: Double,
        rep: RepLevel?,
        commission: Boolean
    ): RetrofitData? {
        if (Helper.anyNull(id, fitter, source, target, rep)) return null

        // Recalculate cost if there's a market
        var c = cost
        var r: RepLevel? = rep
        var com = commission
        if (entity != null && !entity.market.isPlanetConditionMarketOnly) {
            c = RetrofitsKeeper.calculateCost(source, target, entity.market)
        }

        // Target hull may not be available at a given NPC retrofitter
        if (!faction.isPlayerFaction && offerings != null && !offerings.contains(target)) return null

        // Ignore rep level and commission for player
        if (faction.id.equals(Factions.PLAYER)) {
            r = RepLevel.VENGEFUL
            com = false
        }

        // Some other factions ignore rep and/or commission
//        if (MagicSettings.getList(Settings.MAGIC_ID, Settings.APR_NO_REP).contains(faction.id)) {
//            rep = RepLevel.VENGEFUL
//        }
//        if (MagicSettings.getList(Settings.MAGIC_ID, Settings.APR_NO_COM).contains(faction.id)) {
//            commission = false
//        }

        // Factions that don't offer commissions don't consider commissions, naturally
        if (!faction.getCustomBoolean(Factions.CUSTOM_OFFERS_COMMISSIONS)) {
            com = false
        }
        return RetrofitData(
            id!!, fitter!!, source!!, target!!, c,
            0.0, r!!, com
        )
    }
}