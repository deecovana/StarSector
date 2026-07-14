package roiderUnion.nomads.old

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.*
import roiderUnion.nomads.NomadsData
import roiderUnion.nomads.NomadsHelper
import roiderUnion.nomads.old.bases.NomadBaseLevel
import roiderUnion.nomads.old.bases.NomadBaseLevelTracker
import java.util.*

class NomadsReturnTask(private val nomads: NomadsData, private val base: SectorEntityToken) : Runnable {
    override fun run() {
        cleanupEvents()
        upgradeBase()
        deactivateBase()
        returnNomads()
    }

    private fun cleanupEvents() {
        // do any required cleanup on lingering events
    }

    private fun deactivateBase() {
        Memory.unset(MemoryKeys.NOMAD_BASE_ACTIVE, base)
        if (Memory.contains(MemoryKeys.NOMAD_BASE_UPGRADING, base)) {
            Memory.set(MemoryKeys.NOMAD_BASE_UPGRADING, true, base)
        }
        val imageCat = base.customInteractionDialogImageVisual?.spriteId?.category
        val imageKey = base.customInteractionDialogImageVisual?.spriteId?.key

        base.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.ORBITAL_CONSTRUCTION) // extern
        NomadsHelper.removeOrbitingNomads(base)
    }

    private fun upgradeBase() {
        if (!Memory.isFlag(MemoryKeys.NOMAD_BASE_UPGRADING, base)) return
        Memory.unset(MemoryKeys.NOMAD_BASE_UPGRADING, base)

        val currLevel = NomadBaseLevelTracker.getBaseLevel(base)
        val isUnionCapital = Memory.isFlag(MemoryKeys.NOMAD_BASE_UNION_CAPITAL, base)
        val isMaxLevel = (currLevel == NomadBaseLevel.ORBITAL && base.faction.id != RoiderFactions.ROIDER_UNION)
                || (currLevel == NomadBaseLevel.BATTLESTATION && !isUnionCapital)
        if (isMaxLevel) return

        val newLevel = NomadBaseLevelTracker.getNextLevel(currLevel)
        NomadBaseLevelTracker.setBaseLevel(base, newLevel)
    }

    private fun returnNomads() {
        nomads.level = nomads.level.next()
        addReturnRoutes()
        nomads.location = null
    }


    private fun addReturnRoutes() {
        val numClusterRoutes = NomadsHelper.pickNumNomadRoutes(nomads.level)
        val numWandererRoutes = NomadsHelper.pickNumNomadWandererRoutes(numClusterRoutes)

        val spawner = GatheringFleetSpawner(nomads.level)
        for (i in numClusterRoutes downTo 0) {
            val dest = NomadsHelper.pickMigrationSource() ?: continue
            addRoute(base, dest, spawner)
        }

        for (i in numWandererRoutes downTo 0) {
            val source = NomadsHelper.pickWanderersSource(base) ?: continue
            val core = NomadsHelper.pickMigrationSource() ?: continue
            val dest = NomadsHelper.pickDestOrNearby(core)
            addRoute(source, dest, spawner)
        }
    }

    private fun addRoute(source: SectorEntityToken, dest: SectorEntityToken, spawner: RouteManager.RouteFleetSpawner) {
        val extra = RouteManager.OptionalFleetData(source.market, Factions.INDEPENDENT)
        extra.fleetType = RoiderFleetTypes.NOMAD_FLEET
        extra.fp = GatheringTask.getFP(nomads.level, Helper.random.nextBoolean())
        extra.strength = Misc.getAdjustedStrength(extra.fp, source.market)
        val custom = GatheringFleetData(extra.fp)
        val route = RouteManager.getInstance().addRoute(
            NomadsScript.ROUTE_SOURCE_ID,
            source.market,
            Helper.random.nextLong(),
            extra,
            spawner,
            custom
        )

        val travelDays = RouteLocationCalculator.getTravelDays(source, dest)

        var index = 0
        route.addSegment(RouteManager.RouteSegment(++index, getOrbitDays(Helper.random), source))
        route.addSegment(RouteManager.RouteSegment(++index, travelDays, source, dest))
        route.addSegment(RouteManager.RouteSegment(++index, getOrbitDays(Helper.random), dest))
    }

    private fun getOrbitDays(random: Random): Float = NomadsScript.BASE_ORBIT_DAYS + NomadsScript.BASE_ORBIT_DAYS * random.nextFloat()

}
