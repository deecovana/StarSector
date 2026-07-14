package retroLib.impl

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import retroLib.*
import retroLib.api.RetrofitAdjuster
import retroLib.api.RetrofitFilter

open class BaseRetrofitFilter(protected val market: MarketAPI?, override val adjuster: RetrofitAdjuster) :
    RetrofitFilter {
    override fun allow(data: RetrofitData?): Boolean {
        if (data == null) return false
        val newData = adjuster.getFilterTestData(data)
        if (!allowHidden(newData)) return false
        if (Helper.isFrameHull(newData.targetSpec)) return true
        return if (newData.tags.contains(RetroLib_Tags.INDUSTRY)) Helper.marketHasHeavyIndustry(market)
        else true
    }

    protected open fun allowHidden(data: RetrofitData): Boolean {
        if (isHiddenShip(data.sourceSpec)) return false
        if (isHiddenShip(data.targetSpec)) return false
        if (isHiddenWing(data.sourceWingSpec)) return false
        if (isHiddenWing(data.targetWingSpec)) return false
        return data.tags.none { it == RetroLib_Tags.HIDDEN }
    }

    protected fun isHiddenShip(spec: ShipHullSpecAPI?): Boolean {
        val hints = spec?.hints ?: setOf()
        return hints.contains(ShipTypeHints.HIDE_IN_CODEX) || hints.contains(ShipTypeHints.UNBOARDABLE)
    }

    protected fun isHiddenWing(spec: FighterWingSpecAPI?): Boolean {
        val tags = spec?.tags ?: setOf()
        return tags.contains(Tags.RESTRICTED) || isHiddenShip(spec?.variant?.hullSpec)
    }
}