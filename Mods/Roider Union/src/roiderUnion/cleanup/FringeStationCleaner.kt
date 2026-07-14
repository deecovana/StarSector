package roiderUnion.cleanup

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.Misc
import scripts.campaign.bases.Roider_RoiderBaseIntelV2

/**
 * Author: SafariJohn
 */
object FringeStationCleaner {
    fun removeOrphanedFringeStations(sector: SectorAPI) {
        val orphans: MutableList<SectorEntityToken> = ArrayList()

        // Find orphans
        for (loc in sector.allLocations) {
            for (entity in loc.allEntities) {
                val market = entity.market ?: continue
                if (market.id.startsWith(Roider_RoiderBaseIntelV2.Companion.MARKET_PREFIX)
                    && !market.isInEconomy
                ) {
                    orphans.add(entity)
                }
            }
        }

        // Clean up orphans
        for (entity in orphans) {
            Global.getLogger(FringeStationCleaner::class.java)
                .info("Cleaning up orphaned fringe station at " + entity.starSystem.name)
            val fleet = Misc.getStationFleet(entity)
            fleet?.despawn()
            val market = entity.market
            market.connectedEntities.remove(entity)
            entity.market = null
            Misc.fadeAndExpire(entity)
        }
    }
}