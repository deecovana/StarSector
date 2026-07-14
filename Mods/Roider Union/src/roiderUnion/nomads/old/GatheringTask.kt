package roiderUnion.nomads.old

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteFleetSpawner
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.TaskTimer
import roiderUnion.ids.*
import roiderUnion.nomads.*
import java.util.*
import kotlin.math.max

class GatheringTask(private val nomads: NomadsData, private val base: SectorEntityToken) : Runnable {
    companion object {
        const val NOMADS_RETURN_TIME = 30f
        const val MAX_OUTPOSTS = 5

        fun getFP(nomadsLevel: NomadsLevel, smallFleet: Boolean): Float {
            val baseFp = when (nomadsLevel) {
                NomadsLevel.MINOR -> 80f
                NomadsLevel.MAJOR -> 120f
                NomadsLevel.ALLIED -> 140f
                NomadsLevel.ELITE -> 150f
                NomadsLevel.UNION -> 200f
            }
            val mult = if (smallFleet) max(0.2f, Helper.random.nextFloat()) else 1f

            return baseFp * mult + Helper.random.nextFloat() * (40f * mult / 2f) // 40f?
        }
    }

    val random = Helper.random

    override fun run() {
        activateBase()
        buildOutposts()
        createEvents()
        dispatchNomads()
    }

    private fun activateBase() {
        if (!Memory.contains(MemoryKeys.NOMAD_BASE_UPGRADING, base)) {
            Memory.set(MemoryKeys.NOMAD_BASE_UPGRADING, false, base)
        }
        Memory.set(MemoryKeys.NOMAD_BASE_ACTIVE, true, NOMADS_RETURN_TIME, base) // is this in days or seconds?
        Helper.sector?.addScript(TaskTimer(NomadsReturnTask(nomads, base), NOMADS_RETURN_TIME))

        base.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.HOUND_HANGAR)
        NomadsHelper.addOrbitingNomads(base)
    }

    private fun buildOutposts() {
        val missingOutposts = MAX_OUTPOSTS - countOutposts()
        if (missingOutposts > 0) {
            for (i in missingOutposts downTo 1) {
//                val outpostScript = NomadOutpost(base.starSystem, base.faction.id)
//                Helper.sector?.addScript(outpostScript)
                val loc = BaseThemeGenerator.pickCommonLocation(
                    Helper.random,
                    base.starSystem,
                    0f,
                    true,
                    null
                )
                val types = WeightedRandomPicker<String>(Helper.random)
                types.add("station1_Standard")
                val outpost = NomadsHelper.addBattlestation(base.starSystem, loc, types)
                base.starSystem.addEntity(outpost)
            }
        }
    }

    private fun countOutposts(): Int {
        return base.starSystem?.allEntities?.count { it.hasTag(RoiderTags.NOMAD_OUTPOST) } ?: 0
    }

    private fun createEvents() {
        // Set up events

    }

    private fun dispatchNomads() {
        nomads.location = NomadsHelper.pickMigrationSource()
        addSourceRoutes()
        nomads.location = base
    }


    private fun addSourceRoutes() {
        val numClusterRoutes = NomadsHelper.pickNumNomadRoutes(nomads.level)
        val numWandererRoutes = NomadsHelper.pickNumNomadWandererRoutes(numClusterRoutes)

        val spawner = GatheringFleetSpawner(nomads.level)
        for (i in numClusterRoutes downTo 0) {
            val source = nomads.location ?: continue
            addRoute(source, spawner)
        }

        for (i in numWandererRoutes downTo 0) {
            val source = NomadsHelper.pickMigrationSource() ?: continue
            addRoute(source, spawner)
        }
    }

    private fun addRoute(source: SectorEntityToken, spawner: RouteFleetSpawner) {
        val extra = RouteManager.OptionalFleetData(source.market, Factions.INDEPENDENT)
        extra.fleetType = RoiderFleetTypes.NOMAD_FLEET
        extra.fp = getFP(nomads.level, random.nextBoolean())
        extra.strength = Misc.getAdjustedStrength(extra.fp, source.market)
        val custom = GatheringFleetData(extra.fp)
        val route = RouteManager.getInstance().addRoute(
            NomadsScript.ROUTE_SOURCE_ID,
            source.market,
            random.nextLong(),
            extra,
            spawner,
            custom
        )

        val dest = NomadsHelper.pickDestOrNearby(base)
        val travelDays = RouteLocationCalculator.getTravelDays(source, dest)

        var index = 0
        route.addSegment(RouteManager.RouteSegment(++index, getOrbitDays(random), source))
        route.addSegment(RouteManager.RouteSegment(++index, travelDays, source, dest))
        route.addSegment(RouteManager.RouteSegment(++index, getOrbitDays(random), dest))
    }

    private fun getOrbitDays(random: Random): Float = NomadsScript.BASE_ORBIT_DAYS + NomadsScript.BASE_ORBIT_DAYS * random.nextFloat()
}
