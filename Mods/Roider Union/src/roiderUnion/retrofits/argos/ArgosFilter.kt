package roiderUnion.retrofits.argos

import retroLib.RetrofitData
import retroLib.api.RetrofitAdjuster
import retroLib.impl.BaseRetrofitFilter
import roiderUnion.helpers.Helper
import roiderUnion.ids.Fitters
import roiderUnion.ids.RoiderFactions
import roiderUnion.nomads.NomadsData

class ArgosFilter(private val nomadGroup: NomadsData?, adjuster: RetrofitAdjuster) : BaseRetrofitFilter(null, adjuster) {
    override fun allow(data: RetrofitData?): Boolean {
        if (data == null) return false
        if (!super.allow(data)) return false
        if (data.tags.contains(RoiderFactions.ROIDER_UNION) && retroLib.Helper.isFrameHull(data.targetSpec)) return true
        if (data.tags.contains(Fitters.ARGOS) && getOfferings().contains(data.target)) return true
        return false
    }

    private fun getOfferings(): Collection<String> {
        if (nomadGroup == null) {
            val playerFaction = Helper.sector?.playerFaction ?: return emptyList()
            return playerFaction.knownShips + playerFaction.knownFighters
        } else {
            return nomadGroup.knownBPs
        }
    }
}