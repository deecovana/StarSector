package roiderUnion.retrofits.argos

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import retroLib.RetroLib_Tags
import retroLib.RetrofitData
import retroLib.api.RetrofitFilter
import retroLib.impl.BaseRetrofitManager
import roiderUnion.helpers.Helper
import roiderUnion.ids.ShipsAndWings

class ArgosRetrofitManager(faction: FactionAPI, filter: RetrofitFilter) : BaseRetrofitManager(null, faction, filter) {
    override fun isTargetAllowed(targetId: String?): Boolean {
        val data = retrofits.firstOrNull { it.target == targetId } ?: return false
        if (retroLib.Helper.isFrameHull(data.targetSpec)) return true
        if (Helper.sector?.playerFaction?.knowsShip(targetId) == true) return true
        if (Helper.sector?.playerFaction?.knowsFighter(targetId) == true) return true
        return false
    }

    override fun getAvailableTargets(): List<FleetMemberAPI> {
        val result = mutableListOf<FleetMemberAPI>()
        val playerFleet = retroLib.Helper.sector?.playerFleet?.membersWithFightersCopy ?: listOf()
        val playerFleetFiltered = if (isLastArgos()) playerFleet.filter { it.hullSpec?.baseHullId != ShipsAndWings.ARGOS }
            else playerFleet
        val playerWingIds = playerFleetFiltered.filter { it.isFighterWing }.map { it.specId }
        result.addAll(retrofits.asSequence()
            .filter { isArgosTargetShipAvailable(it, playerFleetFiltered) }
            .filter { isTargetAllowed(it.target) && isTargetLegal(it.target) }
            .mapNotNull { retroLib.Helper.createShip(it.target) }
        )
        result.addAll(retrofits
            .filter { playerWingIds.contains(it.source) }
            .filter { isTargetAllowed(it.target) && isTargetLegal(it.target) }
            .mapNotNull { retroLib.Helper.createWing(it.target) }
        )
        return result
            .distinctBy { it.specId }
            .sortedBy { it.hullSpec.hullName }
            .sortedByDescending { it.hullSpec.hullSize }
    }

    override fun getAvailableFrameSources(target: ShipHullSpecAPI): List<FleetMemberAPI> {
        val result = super.getAvailableFrameSources(target)
        return if (isLastArgos()) result.filter { it.hullSpec?.baseHullId != ShipsAndWings.ARGOS }
            else result
    }

    private fun isArgosTargetShipAvailable(data: RetrofitData, playerFleet: List<FleetMemberAPI>): Boolean {
        val playerHullIds = playerFleet.map { it.hullId } + playerFleet.mapNotNull { it.hullSpec?.baseHullId }
        val playerShipSizes = playerFleet.mapNotNull { it.hullSpec?.hullSize }.toSet()
        val largestFrame = getLargestFrameSize(playerFleet)
        return playerHullIds.contains(data.source)
                || (!data.tags.contains(RetroLib_Tags.FIGHTER_WING) && data.targetSpec.hullSize <= largestFrame)
                || (retroLib.Helper.isFrameHull(data.targetSpec) && playerShipSizes.contains(data.targetSpec.hullSize))
    }

    override fun isSourceAllowed(data: RetrofitData): Boolean {
        if (data.sourceSpec?.baseHullId == ShipsAndWings.ARGOS && isLastArgos()) return false
        if (retroLib.Helper.isFrameHull(data.targetSpec)) return true
        if (Helper.sector?.playerFaction?.knowsShip(data.target) == true) return true
        if (Helper.sector?.playerFaction?.knowsFighter(data.target) == true) return true
        return false
    }

    private fun isLastArgos(): Boolean {
        val playerFleet = Helper.sector?.playerFleet ?: return false
        val result =  playerFleet.fleetData?.membersListWithFightersCopy?.count { it.hullSpec?.baseHullId == ShipsAndWings.ARGOS } == 1
        return result
    }
}