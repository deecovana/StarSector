package roiderUnion.world.systems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.*
import roiderUnion.ids.RoiderIds.DESC
import roiderUnion.ids.systems.AtkaIds
import roiderUnion.world.*
import java.awt.Color

object Atka {
    fun generate() {
        val system = Helper.sector?.createStarSystem(AtkaIds.SYSTEM_NAME) ?: return
        system.backgroundTextureFilename = AtkaIds.BACKGROUND
        val star: PlanetAPI = initStar(system)
        addVoidResources(system)
        buildInnerSystem(system, star)
        buildKalekhtaSystem(system, star)
        buildOuterSystem(system, star)
        system.autogenerateHyperspaceJumpPoints(true, false)
        SectorGenHelper.clearNearbyHyperspace(system)
    }

    private fun initStar(system: StarSystemAPI): PlanetAPI {
        val result = SectorGenHelper.initStar(system,
            EntityLayout(AtkaIds.PRIMARY, 400f, coronaRadius = 200f)
        )
        result.spec.glowTexture = Global.getSettings().getSpriteName(Categories.HAB_GLOWS, HabGlows.BANDED)
        result.spec.glowColor = (Color(255, 235, 50, 128))
        result.spec.atmosphereThickness = 0.5f
        result.applySpecChanges()

        SectorGenHelper.buildMagneticRing(system, result,
            MagneticFieldLayout(700f, 1200f, 150f)
        )
        system.lightColor = Color(200, 180, 160)
        return result
    }

    private fun addVoidResources(system: StarSystemAPI) {
        SectorGenHelper.addVoidResources(
            system,
            Pair(Commodities.ORE, Conditions.ORE_ULTRARICH),
            Pair(Commodities.RARE_ORE, Conditions.RARE_ORE_ABUNDANT)
        )
    }

    private fun buildInnerSystem(system: StarSystemAPI, star: PlanetAPI) {
        buildAkutan(system, star)
        buildKorovin(system, star)
        buildPointCheerful(system, star)
    }

    private fun buildAkutan(system: StarSystemAPI, star: PlanetAPI) {
        val akutan: PlanetAPI = SectorGenHelper.buildPlanet(system, star,
            EntityLayout(AtkaIds.AKUTAN, 240f, 10f, 1550f, 30f)
        )
        akutan.spec.planetColor = Color(245, 38, 8, 255)
        akutan.spec.glowTexture = Global.getSettings().getSpriteName(Categories.HAB_GLOWS, HabGlows.BANDED)
        akutan.spec.glowColor = (Color(235, 38, 8, 145))
        akutan.spec.isUseReverseLightForGlow = true
        akutan.spec.atmosphereThickness = 0.5f
        akutan.spec.cloudRotation = 15f
        akutan.spec.atmosphereColor = Color(238, 18, 55, 245)
        akutan.spec.pitch = -5f
        akutan.spec.tilt = 40f
        akutan.applySpecChanges()
        akutan.customDescriptionId = (AtkaIds.AKUTAN.id + DESC)
        akutan.autogenJumpPointNameInHyper = SectorGenHelper.getGravWellName(system, akutan)

        SectorGenHelper.buildCoveringMagneticField(
            system,
            akutan,
            250f,
            100f,
            Color(50, 20, 100, 40),
            1f
        )

        SectorGenHelper.buildInSystemJumpPoint(system, star,
            EntityLayout(
                AtkaIds.JP_INNER,
                angle = SectorGenHelper.getTrailingLagrangeL5(akutan.circularOrbitAngle),
                orbitRadius = akutan.circularOrbitRadius,
                orbitDays = akutan.circularOrbitPeriod
            )
        )
    }

    private fun buildKorovin(system: StarSystemAPI, star: PlanetAPI) {
        val korovin: PlanetAPI = SectorGenHelper.buildPlanet(system, star,
            EntityLayout(AtkaIds.KOROVIN, 200f, 55f, 3700f)
        )
        korovin.spec.glowTexture = Global.getSettings().getSpriteName(Categories.HAB_GLOWS, HabGlows.ASHARU)
        korovin.spec.glowColor = (Color(255, 255, 255, 255))
        korovin.spec.isUseReverseLightForGlow = (true)
        korovin.applySpecChanges()
        korovin.customDescriptionId = (AtkaIds.KOROVIN.id + DESC)
        korovin.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.IND_MEGAFACILITY)

        val roiderStation = SectorGenHelper.buildCustomEntity(system, korovin,
            EntityLayout(
                AtkaIds.ROCKPIPER_PERCH,
                angle = SectorGenHelper.ROCKPIPER_ORBIT_ANGLE,
                orbitRadius = korovin.radius + 160f,
                orbitDays = Float.MAX_VALUE
            )
        )
        roiderStation.customDescriptionId = AtkaIds.ROCKPIPER_PERCH.id + DESC

        SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(
                AtkaIds.SP_KOROVIN_L4,
                angle = SectorGenHelper.getLeadingLagrangeL4(korovin.circularOrbitAngle),
                orbitRadius = korovin.circularOrbitRadius,
                orbitDays = korovin.circularOrbitPeriod
            )
        )

        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(
                ExternalStrings.KOROVIN_L5_TROJANS, 500f, 700f,
                SectorGenHelper.getTrailingLagrangeL5(korovin.circularOrbitAngle),
                korovin.circularOrbitRadius,
                korovin.circularOrbitPeriod
            ),
            25, 30, 4f, 16f
        )
    }

    private fun buildPointCheerful(system: StarSystemAPI, star: PlanetAPI) {
        val innerRadius = 5000f
        SectorGenHelper.buildCoverRing(system, star,
            RingParams(
                456f, innerRadius + 100f,
                null, AtkaIds.POINT_CHEERFUL.name
            )
        )
        SectorGenHelper.buildDecoRing(system, star, RingLayout(Rings.DUST_0, 0, innerRadius))
        SectorGenHelper.buildDecoRing(system, star, RingLayout(Rings.DUST_0, 1, innerRadius + 100f))
        SectorGenHelper.buildDecoRing(system, star, RingLayout(Rings.DUST_0, 2, innerRadius + 200f))
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(innerRadius, 150f, 128f),
            100
        )

        SectorGenHelper.buildCoverRing(system, star,
            RingParams(
                456f, innerRadius + 400f,
                null, AtkaIds.POINT_CHEERFUL.name
            )
        )
        SectorGenHelper.buildDecoRing(system, star, RingLayout(Rings.DUST_0, 3, innerRadius + 300f))
        SectorGenHelper.buildDecoRing(system, star, RingLayout(Rings.DUST_0, 2, innerRadius + 400f))
        SectorGenHelper.buildDecoRing(system, star, RingLayout(Rings.ICE_0, 2, innerRadius + 500f))
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(innerRadius + 450f, 188f),
            100
        )
    }

    private fun buildKalekhtaSystem(system: StarSystemAPI, star: PlanetAPI) {
        val kalekhta = SectorGenHelper.buildPlanet(system, star,
            EntityLayout(AtkaIds.KALEKHTA, 290f, 130f, 8000f)
        )
        kalekhta.spec.planetColor = Color(255, 210, 170, 255)
        kalekhta.spec.pitch = 20f
        kalekhta.spec.tilt = 10f
        kalekhta.applySpecChanges()
        kalekhta.autogenJumpPointNameInHyper = SectorGenHelper.getGravWellName(system, kalekhta)

        SectorGenHelper.buildPlanet(system, kalekhta,
            EntityLayout(AtkaIds.PRIEST, 30f, 30f, kalekhta.radius + 100f, 30f)
        )
        SectorGenHelper.buildDecoRing(system, kalekhta,
            RingLayout(
                Rings.ASTEROIDS_0,
                3,
                kalekhta.radius + 510f,
                color = Color(170, 210, 255, 255)
            )
        )
        SectorGenHelper.buildAsteroidBelt(system, kalekhta,
            BeltLayout(kalekhta.radius + 510f, 200f),
            50
        )

        SectorGenHelper.buildInSystemJumpPoint(system, star,
            EntityLayout(
                AtkaIds.JP_OUTER,
                angle = SectorGenHelper.getLeadingLagrangeL4(kalekhta.circularOrbitAngle),
                orbitRadius = kalekhta.circularOrbitRadius,
                orbitDays = kalekhta.circularOrbitPeriod
            )
        )
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(
                ExternalStrings.KALEKHTA_L4_TROJANS, 400f, 600f,
                SectorGenHelper.getLeadingLagrangeL4(kalekhta.circularOrbitAngle),
                kalekhta.circularOrbitRadius, kalekhta.circularOrbitPeriod
            ),
            30, 40, 4f, 16f
        )

        SectorGenHelper.buildPlanet(system, kalekhta,
            EntityLayout(
                AtkaIds.MAKUSHIN,
                80f,
                SectorGenHelper.getTrailingLagrangeL5(kalekhta.circularOrbitAngle),
                kalekhta.circularOrbitRadius, kalekhta.circularOrbitPeriod
            )
        )
    }

    private fun buildOuterSystem(system: StarSystemAPI, star: PlanetAPI) {
        buildOuterRim(system, star)
        buildColdRock(system, star)
    }

    private fun buildOuterRim(system: StarSystemAPI, star: PlanetAPI) {
        val rimRadius = 10000f
        val orbitFocus = SectorGenHelper.buildOrbitingToken(system, star,
            EntityLayout(
                EntityIds.EMPTY,
                angle = 45f,
                orbitRadius = 750f,
                orbitDays = 80f
            )
        )
        SectorGenHelper.buildCustomEntity(system, orbitFocus,
            EntityLayout(
                AtkaIds.SP_OUTER,
                angle = -120f,
                orbitRadius = rimRadius
            )
        )

        SectorGenHelper.buildCoverRing(system, orbitFocus,
            RingParams(
                456f, rimRadius + 880f,
                null, AtkaIds.OUTER_RIM.name
            )
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ICE_0, 1, rimRadius + 750f, 300f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 2, rimRadius + 850f, 530f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 1, rimRadius + 950f, 320f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ICE_0, 0, rimRadius + 1010f, 480f)
        )
        SectorGenHelper.buildAsteroidBelt(system, orbitFocus,
            BeltLayout(rimRadius + 880f, 256f),
            200
        )

        SectorGenHelper.buildCoverRing(system, orbitFocus,
            RingParams(
                356f, rimRadius + 1320f,
                null, AtkaIds.OUTER_RIM.name
            )
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 0, rimRadius + 1050f, 340f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ICE_0, 3, rimRadius + 1143f, 420f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ICE_0, 3, rimRadius + 1393f, 420f)
        )
        SectorGenHelper.buildAsteroidBelt(system, orbitFocus,
            BeltLayout(rimRadius + 1570f, 256f),
            200
        )

        SectorGenHelper.buildCoverRing(system, orbitFocus,
            RingParams(
                456f, rimRadius + 1650f,
                null, AtkaIds.OUTER_RIM.name
            )
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ASTEROIDS_0, 1, rimRadius + 1513f, 560f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 0, rimRadius + 1613f, 620f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 1, rimRadius + 1743f, 380f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ASTEROIDS_0, 3, rimRadius + 1843f, 780f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 1, rimRadius + 1938f, 400f)
        )

        SectorGenHelper.buildCoverRing(system, orbitFocus,
            RingParams(
                656f, rimRadius + 2250f,
                null, AtkaIds.OUTER_RIM.name
            )
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 3, rimRadius + 2095f, 430f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ASTEROIDS_0, 1, rimRadius + 2263f, 600f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 0, rimRadius + 2399f, 460f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.DUST_0, 3, rimRadius + 2494f, 300f)
        )
        SectorGenHelper.buildDecoRing(system, orbitFocus,
            RingLayout(Rings.ASTEROIDS_0, 0, rimRadius + 2494f, 790f)
        )
        SectorGenHelper.buildAsteroidBelt(system, orbitFocus,
            BeltLayout(rimRadius + 2150f, 256f),
            300
        )
        SectorGenHelper.buildAsteroidBelt(system, orbitFocus,
            BeltLayout(rimRadius + 2450f, 188f),
            300
        )
        SectorGenHelper.buildAsteroidBelt(system, orbitFocus,
            BeltLayout(rimRadius + 2750f, 128f),
            300
        )
    }

    private fun buildColdRock(system: StarSystemAPI, star: PlanetAPI) {
        val startingAngle = 180f
        val orbitRadius = 13150f
        val orbitDays = 600f
        val orbitFocus = SectorGenHelper.buildOrbitingToken(system, star,
            EntityLayout(
                EntityIds.EMPTY,
                angle = startingAngle,
                orbitRadius = 2000f,
                orbitDays = orbitDays
            )
        )
        val coldRock = SectorGenHelper.buildPlanet(system, orbitFocus,
            EntityLayout(AtkaIds.COLD_ROCK, 120f, startingAngle, orbitRadius, orbitDays)
        )
        SectorGenHelper.buildRing(system, coldRock,
            RingLayout(Rings.ICE_0, 2, coldRock.radius + 305f, 45f)
        )
        val bastion = SectorGenHelper.buildCustomEntity(system, coldRock,
            EntityLayout(
                AtkaIds.COLD_ROCK_BASTION,
                angle = 45f,
                orbitRadius = coldRock.radius + 80f,
                orbitDays = 30f
            )
        )
        bastion.customDescriptionId = (AtkaIds.COLD_ROCK_BASTION.id + DESC)
        SectorGenHelper.buildInSystemJumpPoint(system, orbitFocus,
            EntityLayout(
                AtkaIds.JP_FRINGE,
                angle = SectorGenHelper.getLeadingLagrangeL4(startingAngle),
                orbitRadius = orbitRadius,
                orbitDays = orbitDays
            )
        )
    }
}