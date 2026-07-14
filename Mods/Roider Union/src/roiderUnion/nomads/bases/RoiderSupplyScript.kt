package roiderUnion.nomads.bases

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.Aliases
import roiderUnion.ids.MemoryKeys

class RoiderSupplyScript(private val base: SectorEntityToken) : EconomyUpdateListener {
    companion object {
        fun alias(x: XStream) {
            val jClass = RoiderSupplyScript::class.java
            x.alias(Aliases.SPLYSCRIPT, jClass)
            x.aliasAttribute(jClass, "base", "b")
            x.aliasAttribute(jClass, "market", "m")
            x.aliasAttribute(jClass, "modId", "id")
        }
    }

    private val market = base.market
    private val modId = market.id + MemoryKeys.NOMAD_BASE

    override fun commodityUpdated(commodityId: String?) {}

    override fun economyUpdated() {
        var qualityBonus = 0f
        var fleetSizeBonus = 1f
        when (NomadBaseLevelTracker.getLevel(base)) {
            NomadBaseLevel.STARTING -> {
                qualityBonus = 0f
                fleetSizeBonus = 0.2f
            }
            NomadBaseLevel.ESTABLISHED -> {
                qualityBonus = 0.2f
                fleetSizeBonus = 0.3f
            }
            NomadBaseLevel.BATTLESTATION -> {
                qualityBonus = 0.2f
                fleetSizeBonus = 0.4f
            }
            NomadBaseLevel.SHIPWORKS -> {
                qualityBonus = 0.2f
                fleetSizeBonus = 0.5f
            }
            NomadBaseLevel.HQ -> {
                qualityBonus = 0.2f
                fleetSizeBonus = 0.75f
            }
            NomadBaseLevel.CAPITAL -> {
                qualityBonus = 0.0f
                fleetSizeBonus = 0.75f
            }
        }

        market.stats?.dynamic?.getMod(Stats.FLEET_QUALITY_MOD)?.modifyFlatAlways(
            modId, qualityBonus,
            ExternalStrings.NOMAD_DEVELOPMENT
        )

        market.stats?.dynamic?.getMod(Stats.COMBAT_FLEET_SIZE_MULT)?.modifyFlatAlways(
            modId,
            fleetSizeBonus,
            ExternalStrings.NOMAD_DEVELOPMENT
        )
    }

    override fun isEconomyListenerExpired(): Boolean = base.isExpired || !base.isAlive
}