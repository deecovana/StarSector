package roiderUnion.world.systems

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import roiderUnion.helpers.Helper
import roiderUnion.ids.*
import roiderUnion.ids.RoiderIds.DESC
import roiderUnion.ids.systems.AttuIds
import roiderUnion.world.*
import java.awt.Color

object Attu {
    fun generate() {
        val system = Helper.sector?.createStarSystem(AttuIds.SYSTEM_NAME) ?: return
        system.type = StarSystemGenerator.StarSystemType.BINARY_FAR
        system.backgroundTextureFilename = AttuIds.BACKGROUND
        val star = initStar(system)
        addVoidResources(system)
        system.lightColor = Color(255, 135, 40)
        buildInnerSystem(system, star)
        createAsteroidFields(system, star)
        buildAgattuSystem(system, star)
        system.autogenerateHyperspaceJumpPoints(true, true)
        SectorGenHelper.clearNearbyHyperspace(system)
    }
    
    private fun initStar(system: StarSystemAPI): PlanetAPI {
        val result = SectorGenHelper.initStar(system,
            EntityLayout(AttuIds.PRIMARY, 950f, coronaRadius = 500f),
            12f, 1f, 3f
        )
        return result
    }

    private fun addVoidResources(system: StarSystemAPI) {
        SectorGenHelper.addVoidResources(
            system,
            Pair(Commodities.ORE, Conditions.ORE_RICH),
            Pair(Commodities.RARE_ORE, Conditions.RARE_ORE_ABUNDANT)
        )
    }

    private fun buildInnerSystem(system: StarSystemAPI, star: PlanetAPI) {
        val moffet = SectorGenHelper.buildPlanet(system, star,
            EntityLayout(AttuIds.MOFFET, 150f, 155f, 1820f)
        )
        val jumpPoint = SectorGenHelper.buildInSystemJumpPoint(system, star,
            EntityLayout(
                AttuIds.JP_INNER,
                angle = SectorGenHelper.getTrailingLagrangeL5(moffet.circularOrbitAngle),
                orbitRadius = moffet.circularOrbitRadius,
                orbitDays = moffet.circularOrbitPeriod
            )
        )
        jumpPoint.relatedPlanet = moffet

        val belt1Radius = 2750f
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(belt1Radius, 500f, 80f, 140f),
            90
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 1, belt1Radius - 50f, 110f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.ASTEROIDS_0, 2, belt1Radius + 50f, 100f)

        )

        val belt2Radius = 3400f
        val belt2OrbitDays = 180f
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(belt2Radius, 500f, belt2OrbitDays, belt2OrbitDays * 1.22f),
            90
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 2, belt2Radius - 100f, 130f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.ASTEROIDS_0, 0, belt2Radius + 20f, 120f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 2, belt2Radius + 100f, 130f)
        )

        val holtz = SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(AttuIds.HOLTZ, angle = 180f, orbitRadius = belt2Radius, orbitDays = belt2OrbitDays)
        )
        holtz.customDescriptionId = AttuIds.HOLTZ.id + DESC
        holtz.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.SPACE_WRECKAGE)
        holtz.addTag(Tags.STATION)
        SectorGenHelper.convertOrbitToPointingDown(holtz)
    }

    private fun createAsteroidFields(system: StarSystemAPI, star: PlanetAPI) {
        val innerRadius = 4810f
        val innerOrbitDays = 146f
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 700f, 800f, 240f, innerRadius, innerOrbitDays),
            30, 40, 4f, 16f
        )
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 700f, 800f, 40f, innerRadius, innerOrbitDays),
            30, 40, 4f, 16f
        )
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 700f, 800f, 80f, innerRadius, innerOrbitDays),
            30, 40, 4f, 16f
        )

        val outerRadius = 7020f
        val outerOrbitDays = 202f
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 700f, 800f, -10f, outerRadius, outerOrbitDays),
            30, 40, 4f, 16f
        )
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 700f, 800f, 80f, outerRadius, outerOrbitDays),
            30, 40, 4f, 16f
        )
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 700f, 800f, 130f, outerRadius, outerOrbitDays),
            30, 40, 4f, 16f
        )
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(null, 700f, 800f, 200f, outerRadius, outerOrbitDays),
            30, 40, 4f, 16f
        )

        SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(AttuIds.COMM_RELAY, angle = -80f, orbitRadius = outerRadius, orbitDays = outerOrbitDays)
        )
    }

    private fun buildAgattuSystem(system: StarSystemAPI, star: PlanetAPI) {
        val agattu = SectorGenHelper.initSecondary(system, star,
            EntityLayout(AttuIds.SECONDARY, 350f, 65f, 10600f, 600f, 150f),
            2f, 0f, 1f
        )
        SectorGenHelper.buildMagneticRing(system, agattu,
            MagneticFieldLayout(
                agattu.radius * 3f - 250f,
                agattu.radius * 3f + 250f
            )
        )
        val beltRadius = 1750f
        SectorGenHelper.buildAsteroidBelt(system, agattu,
            BeltLayout(beltRadius, 500f),
            90
        )
        SectorGenHelper.buildDecoRing(system, agattu,
            RingLayout(Rings.DUST_0, 2, beltRadius - 50f, 70f)
        )
        SectorGenHelper.buildDecoRing(system, agattu,
            RingLayout(Rings.ASTEROIDS_0, 1, beltRadius + 50f, 60f)
        )

        SectorGenHelper.buildInSystemJumpPoint(system, star,
            EntityLayout(
                AttuIds.JP_OUTER,
                angle = SectorGenHelper.getTrailingLagrangeL5(agattu.circularOrbitAngle),
                orbitRadius = agattu.circularOrbitRadius,
                orbitDays = agattu.circularOrbitPeriod
            )
        )
    }
}