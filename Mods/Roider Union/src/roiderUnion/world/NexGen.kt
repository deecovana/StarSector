package roiderUnion.world

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.ExerelinSetupData
import exerelin.campaign.PlayerFactionStore
import exerelin.campaign.SectorManager
import roiderUnion.ModPlugin
import roiderUnion.helpers.Helper
import roiderUnion.helpers.MarketHelper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIds
import roiderUnion.ids.RoiderIndustries
import roiderUnion.ids.systems.AtkaIds
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object NexGen {
    val isRoiderUnionEnabled: Boolean
        get() {
            if (!ModPlugin.hasNexerelin || SectorManager.getManager().isCorvusMode) return true
            val roidersEnabled = ExerelinSetupData.getInstance()?.factions?.get(RoiderFactions.ROIDER_UNION) == true
            val playerFactionIsRoider = PlayerFactionStore.getPlayerFactionIdNGC() == RoiderFactions.ROIDER_UNION
            return playerFactionIsRoider || roidersEnabled
        }

    fun addNexRandomModeDives() {
        val sector: SectorAPI = Global.getSector()
        val factions: MutableMap<String, Float> = HashMap()
        factions[RoiderFactions.ROIDER_UNION] = 1f
        factions[Factions.HEGEMONY] = 0.1f // Very small chance for Heg dives or Union HQ
        factions[Factions.INDEPENDENT] = 0.4f
        factions[Factions.PIRATES] = 0.2f
        val roiderMarkets: MutableList<MarketAPI> = ArrayList()
        for (market in sector.economy.getMarketsInGroup(null)) {
            if (factions.containsKey(market.factionId)) {
                if (Random().nextFloat() < (factions[market.factionId] ?: 0f)) {
                    roiderMarkets.add(market)
                }
            }
        }
        for (market in roiderMarkets) {
            if (market.starSystem == null) continue
            if (market.primaryEntity == null) continue
            var industryId: String = RoiderIndustries.DIVES
            var milBase = market.getIndustry(Industries.MILITARYBASE)
            if (milBase == null) milBase = market.getIndustry(Industries.HIGHCOMMAND)

            // Replace all Roider Union military with Union HQs
            if (market.factionId == RoiderFactions.ROIDER_UNION) {
                milBase?.unapply()
                market.removeIndustry(milBase?.id ?: "", null, false)
                market.removeSubmarket(Submarkets.GENERIC_MILITARY)
                SectorGenHelper.removeMilBaseCommander(market)
                industryId = RoiderIndustries.UNION_HQ

                // Chance for Union HQ on non-Roider markets
            } else if (!market.faction.isHostileTo(RoiderFactions.ROIDER_UNION)
                && canAddRandomUnionHQ(market.size)
            ) {
                var indCount = market.industries.count { it.isIndustry }
                if (indCount >= Misc.getMaxIndustries(market)) {
                    if (market.hasIndustry(Industries.MILITARYBASE)) {
                        milBase?.unapply()
                        market.removeIndustry(milBase?.id ?: "", null, false)
                        market.removeSubmarket(Submarkets.GENERIC_MILITARY)
                        SectorGenHelper.removeMilBaseCommander(market)
                        indCount--
                    }
                }
                if (indCount < Misc.getMaxIndustries(market)) industryId = RoiderIndustries.UNION_HQ
            }
            SectorGenHelper.addDive(sector, market.starSystem.id, market.primaryEntity.id, industryId)
        }
    }

    private fun canAddRandomUnionHQ(marketSize: Int): Boolean {
        val chance = when (marketSize) { // extern
            1, 2, 3 -> 0.1f
            4 -> 0.4f
            5 -> 0.8f
            6 -> 0.9f
            7 -> 0.8f
            8 -> 0.5f
            9, 10 -> 0.3f
            else -> 0.3f
        }
        return Helper.random.nextFloat() < chance
    }

    fun addNexRandomRockpiper() {
        // Find Roider capital
        var capital: MarketAPI? = null
        for (market in Global.getSector().economy.marketsCopy) {
            if (market.factionId != RoiderFactions.ROIDER_UNION) continue
            if (!market.hasIndustry(RoiderIndustries.UNION_HQ)) continue
            if (capital == null || market.size > capital.size) {
                capital = market
            }
        }
        if (capital == null) return

        capital.getIndustry(RoiderIndustries.UNION_HQ)?.isImproved = true

        Memory.set(MemoryKeys.NOMAD_BASE_UNION_CAPITAL, true, capital)

        // Must have a shipyard
        val hasShipyard = (capital.hasIndustry(Industries.HEAVYINDUSTRY)
                || capital.hasIndustry(Industries.ORBITALWORKS))
        if (!hasShipyard) {
            var added = false

            // First see if it can be added directly
            var indCount = 0
            for (ind in capital.industries) {
                if (ind.isIndustry) indCount++
            }
            if (indCount < Misc.getMaxIndustries(capital)) {
                capital.addIndustry(Industries.ORBITALWORKS)
                added = true
            }

            // Otherwise try to replace something
            if (!added) added = MarketHelper.replaceIndustry(
                capital,
                Industries.COMMERCE, Industries.ORBITALWORKS
            )
            if (!added) added = MarketHelper.replaceIndustry(
                capital,
                Industries.MINING, Industries.ORBITALWORKS
            )
            if (!added) added = MarketHelper.replaceIndustry(
                capital,
                Industries.LIGHTINDUSTRY, Industries.ORBITALWORKS
            )
            if (!added) added = MarketHelper.replaceIndustry(
                capital,
                Industries.FUELPROD, Industries.ORBITALWORKS
            )
            if (!added) added = MarketHelper.replaceIndustry(
                capital,
                Industries.TECHMINING, Industries.ORBITALWORKS
            )
            if (!added) added = MarketHelper.replaceIndustry(
                capital,
                Industries.REFINING, Industries.ORBITALWORKS
            )

            // Failed to add a shipyard
            if (!added) return
        }

        // Time to get to business
        val primary = capital.primaryEntity
        if (primary is PlanetAPI) {
            // ? Increase size to minimum (200), if needed
//            if (planet.getRadius() < 200f) {
//
//                // If the capital is a moon of too small a world, cancel
//                SectorEntityToken parent = planet.getOrbitFocus();
//                if (!(parent.isStar() || parent.isSystemCenter())) {
//                    float parentRadius = parent.getRadius();
//                    if (parentRadius <= 200f) return;
//                }
//
//
//                planet.setRadius(200f);
//            }

            // ? Adjust radius of any satellites, like the junk ring
            // Let's see what happens for now

            // Add Rockpiper Perch and hook it up
            val roiderStation: SectorEntityToken = primary.starSystem.addCustomEntity(
                AtkaIds.ROCKPIPER_PERCH.id,
                AtkaIds.ROCKPIPER_PERCH.name, RoiderIds.Entities.ROCKPIPER_PERCH, RoiderFactions.ROIDER_UNION
            )
            roiderStation.setCircularOrbit(
                primary,
                SectorGenHelper.ROCKPIPER_ORBIT_ANGLE,
                primary.radius + 160f,
                Float.MAX_VALUE
            )
            roiderStation.customDescriptionId = AtkaIds.ROCKPIPER_PERCH.id + RoiderIds.DESC
            roiderStation.market = capital
            capital.connectedEntities.add(roiderStation)

            // If no planet, replace primary entity with Rockpiper Perch
            // Hope this doesn't come up.
        } else {
            val roiderStation: SectorEntityToken = primary.starSystem.addCustomEntity(
                AtkaIds.ROCKPIPER_PERCH.id,
                AtkaIds.ROCKPIPER_PERCH.name, RoiderIds.Entities.ROCKPIPER_PERCH, RoiderFactions.ROIDER_UNION
            )
            roiderStation.customDescriptionId = AtkaIds.ROCKPIPER_PERCH.id + RoiderIds.DESC

            // Save its orbit
            val orbit = primary.orbit
            roiderStation.market = capital
            capital.connectedEntities.add(roiderStation)
            capital.primaryEntity = roiderStation
            capital.connectedEntities.remove(primary)
            if (orbit.focus is PlanetAPI) {
                roiderStation.setCircularOrbit(
                    orbit.focus, primary.circularOrbitAngle,
                    primary.circularOrbitRadius, primary.circularOrbitPeriod
                )
            } else {
                // This could be a weird-ass orbit, but oh well
                roiderStation.setCircularOrbit(
                    orbit.focus, 145f,
                    orbit.focus.radius + 160f, Float.MAX_VALUE
                )
            }

            // Remove the old entity
            Misc.fadeAndExpire(primary)
        }

        // Make sure battlestation attaches
        // And make sure it is a star fortress
        val replaceStations = listOf(
            Industries.STARFORTRESS_HIGH,
            Industries.STARFORTRESS_MID,
            Industries.STARFORTRESS,
            Industries.BATTLESTATION_HIGH,
            Industries.BATTLESTATION_MID,
            Industries.BATTLESTATION,
            Industries.ORBITALSTATION_HIGH,
            Industries.ORBITALSTATION_MID,
            Industries.ORBITALSTATION,
        )
        var replaced = false
        for (station in replaceStations) {
            replaced = MarketHelper.replaceIndustry(capital, station, Industries.STARFORTRESS); break
        }

        if (replaced) capital.advance(1f)
    }
}