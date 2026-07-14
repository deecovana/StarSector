package roiderUnion.world.systems

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.Roider_StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.*
import roiderUnion.ids.RoiderIds.DESC
import roiderUnion.ids.systems.KiskaIds
import roiderUnion.world.*

object Kiska {
    fun generate() {
        val system = Helper.sector?.createStarSystem(KiskaIds.SYSTEM_NAME) ?: return
        val star: PlanetAPI = SectorGenHelper.createNebula(KiskaIds.SYSTEM_ID, system, KiskaIds.AGE)
        buildInnerSystem(system, star)
        addVoidResources(system)
        val radiusAfter = buildLoranSystem(system, star)
        buildOuterSystem(system, star, radiusAfter)
        SectorGenHelper.finalizeNebula(system, star, KiskaIds.AGE, false)
        SectorGenHelper.clearNearbyHyperspace(system)
    }

    private fun addVoidResources(system: StarSystemAPI) {
        SectorGenHelper.addVoidResources(
            system,
            Pair(Commodities.ORE, Conditions.ORE_MODERATE),
            Pair(Commodities.RARE_ORE, Conditions.RARE_ORE_MODERATE),
            Pair(Commodities.ORGANICS, Conditions.ORGANICS_COMMON),
            Pair(Commodities.VOLATILES, Conditions.VOLATILES_ABUNDANT)
        )
    }

    private fun buildInnerSystem(system: StarSystemAPI, star: PlanetAPI) {
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 4900f, 4900f, 0f, 0f),
            220, 330, 4f, 16f
        )

        SectorGenHelper.buildInSystemJumpPoint(system, star,
            EntityLayout(KiskaIds.JP_INNER, angle = 137f, orbitRadius = 4000f)
        )

        val beltRadius = 5200f
        val beltDays = 300f
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(beltRadius + 50f, 500f, beltDays / 2, beltDays + 20f, ExternalStrings.DRUZHININS_BELT),
            90
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 3, beltRadius, beltDays + 5f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.ASTEROIDS_0, 3, beltRadius + 120f, beltDays - 5f)
        )
        val anchorage = SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(KiskaIds.DRUZHININS, angle = 180f, orbitRadius = beltRadius + 400f, orbitDays = beltDays)
        )
        anchorage.customDescriptionId = KiskaIds.DRUZHININS.id + DESC
        anchorage.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.ORBITAL)
        anchorage.addTag(Tags.STATION)
        SectorGenHelper.convertOrbitToPointingDown(anchorage)

        SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(
                KiskaIds.NAV_BUOY, angle = SectorGenHelper.getLeadingLagrangeL4(anchorage.circularOrbitAngle),
            orbitRadius = anchorage.circularOrbitRadius, orbitDays = anchorage.circularOrbitPeriod)
        )
    }

    private fun buildLoranSystem(system: StarSystemAPI, star: PlanetAPI): Float {
        val loran = SectorGenHelper.buildPlanet(system, star,
            EntityLayout(KiskaIds.LORAN, 325f, -50f, 7100f)
        )
        loran.autogenJumpPointNameInHyper = SectorGenHelper.getGravWellName(system, loran, true)
        SectorGenHelper.buildPlanet(system, loran,
            EntityLayout(KiskaIds.GEE, 80f, 30f, 700f)
        )

        val beltRadius = 900f
        val beltDays = 40f
        SectorGenHelper.buildAsteroidBelt(system, loran,
            BeltLayout(beltRadius, 128f, beltDays / 2, beltDays),
            30
        )
        val threeSisters = SectorGenHelper.buildCustomEntity(system, loran,
            EntityLayout(KiskaIds.THREE_SISTERS_STATION, angle = 250f, orbitRadius = beltRadius, orbitDays = beltDays))
        threeSisters.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.HOUND_HANGAR)
        threeSisters.customDescriptionId = KiskaIds.THREE_SISTERS_STATION.id + DESC
        threeSisters.addTag(Tags.STATION)
        SectorGenHelper.convertOrbitToPointingDown(threeSisters)

        SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(
                KiskaIds.COMM_RELAY, angle = SectorGenHelper.getTrailingLagrangeL5(loran.circularOrbitAngle),
            orbitRadius = loran.circularOrbitRadius, orbitDays = loran.circularOrbitPeriod)
        )
        return loran.circularOrbitRadius + beltRadius * 1.25f
    }

    private fun buildOuterSystem(system: StarSystemAPI, orbitFocus: SectorEntityToken, radiusAfter: Float) {
        val radius = Roider_StarSystemGenerator(StarSystemGenerator.CustomConstellationParams(KiskaIds.AGE)).addOrbitingEntitiesRoider(
            system, orbitFocus, KiskaIds.AGE,
            4, 5,
            radiusAfter + 1000f,
            1,
            true
        )
        for (planet in system.planets) {
            if (planet.isGasGiant) {
                planet.autogenJumpPointNameInHyper = SectorGenHelper.getGravWellName(system, planet, true)
            }
        }

        // Fringe JP in asteroid field
        val jumpPoint = SectorGenHelper.buildInSystemJumpPoint(system, orbitFocus,
            EntityLayout(KiskaIds.JP_OUTER, angle = 60f, orbitRadius = radius + 200)
        )
        SectorGenHelper.buildAsteroidField(system, orbitFocus,
            FieldLayout(
                null, 200f, 200f, jumpPoint.circularOrbitAngle,
                jumpPoint.circularOrbitRadius, jumpPoint.circularOrbitPeriod
            ),
            10, 20, 4f, 12f
        )

        // Research Station
        val tritachyonStation = SectorGenHelper.buildCustomEntity(system, orbitFocus,
            EntityLayout(KiskaIds.ZIN_SHIPYARD, -120f, orbitRadius = radius + 800f)
        )
        tritachyonStation.customDescriptionId = KiskaIds.ZIN_SHIPYARD.id + DESC
        tritachyonStation.addTag(Tags.STATION)
        SectorGenHelper.convertOrbitToPointingDown(tritachyonStation)
    }
}