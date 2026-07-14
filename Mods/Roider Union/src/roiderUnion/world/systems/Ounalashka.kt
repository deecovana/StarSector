package roiderUnion.world.systems

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.*
import roiderUnion.ids.RoiderIds.DESC
import roiderUnion.ids.systems.OunalashkaIds
import roiderUnion.world.*

object Ounalashka {
    fun generate() {
        val system = Helper.sector?.createStarSystem(OunalashkaIds.SYSTEM_NAME) ?: return
        system.backgroundTextureFilename = OunalashkaIds.BACKGROUND
        val star = initStar(system)
        addVoidResources(system)
        system.lightColor = SectorGenHelper.interpolateSystemLight(star)
        buildInnerSystem(system, star)
        buildOglodakSystem(system, star)
        buildOuterSystem(system, star)
        StarSystemGenerator.addSystemwideNebula(system, OunalashkaIds.AGE)
        system.autogenerateHyperspaceJumpPoints(true, true)
        SectorGenHelper.clearNearbyHyperspace(system)
    }

    private fun initStar(system: StarSystemAPI): PlanetAPI {
        val result = SectorGenHelper.initStar(system,
            EntityLayout(OunalashkaIds.PRIMARY, 200f, coronaRadius = 300f)
        )
        SectorGenHelper.buildMagneticRing(system, result,
            MagneticFieldLayout(600f, 1100f),
            auroraFreq = 1f
        )
        return result
    }

    private fun addVoidResources(system: StarSystemAPI) {
        SectorGenHelper.addVoidResources(
            system,
            Pair(Commodities.ORE, Conditions.ORE_RICH),
            Pair(Commodities.RARE_ORE, Conditions.RARE_ORE_RICH),
            Pair(Commodities.VOLATILES, Conditions.VOLATILES_DIFFUSE)
        )
    }

    private fun buildInnerSystem(system: StarSystemAPI, star: PlanetAPI) {

        buildPrimaryRings(system, star)
        buildAmaknakSystem(system, star)
    }

    private fun buildPrimaryRings(system: StarSystemAPI, star: PlanetAPI) {
        val ringRadius = 1000f
        val pirateStation = SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(OunalashkaIds.MAGGIES, angle = 176f, orbitRadius = 1700f, orbitDays = 150f)
        )
        pirateStation.setInteractionImage(Categories.ILLUSTRATIONS, Illustrations.ORBITAL_CONSTRUCTION)
        pirateStation.customDescriptionId = OunalashkaIds.MAGGIES.id + DESC
        pirateStation.addTag(Tags.STATION)

        SectorGenHelper.buildCoverRing(system, star,
            RingParams(856f, ringRadius + 300f, null, ExternalStrings.OUNALASHK_RINGS)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 3, ringRadius, 100f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 1, ringRadius + 200f, 80f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 2, ringRadius + 400f, 130f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 1, ringRadius + 600f, 90f)
        )
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(ringRadius + 100f, 256f, 100f, 160f),
            100
        )
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(ringRadius + 500f, 256f, 120f, 180f),
            100
        )

        SectorGenHelper.buildCoverRing(system, star,
            RingParams(456f, ringRadius + 900f, null, ExternalStrings.OUNALASHK_RINGS)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 0, ringRadius + 800f, 80f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 1, ringRadius + 900f, 120f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 2, ringRadius + 1000f, 160f)
        )
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(ringRadius + 950f, 128f, 200f, 300f),
            100
        )

        SectorGenHelper.buildCoverRing(system, star,
            RingParams(456f, ringRadius + 1200f, null, ExternalStrings.OUNALASHK_RINGS)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 3, ringRadius + 1100f, 140f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 2, ringRadius + 1200f, 180f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.DUST_0, 1, ringRadius + 1300f, 220f)
        )
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(ringRadius + 1250f, 188f, 200f, 300f),
            100
        )

        SectorGenHelper.buildCoverRing(system, star,
            RingParams(556f, ringRadius + 1450f, null, ExternalStrings.OUNALASHK_RINGS)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.ICE_0, 0, ringRadius + 1300f, 100f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.ICE_0, 2, ringRadius + 1400f, 140f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.ICE_0, 1, ringRadius + 1500f, 260f)
        )
        SectorGenHelper.buildDecoRing(system, star,
            RingLayout(Rings.ICE_0, 2, ringRadius + 1600f, 180f)
        )
        SectorGenHelper.buildAsteroidBelt(system, star,
            BeltLayout(ringRadius + 1475f, 256f, 200f, 300f),
            100
        )
    }

    private fun buildAmaknakSystem(system: StarSystemAPI, star: PlanetAPI) {
        val amaknak = SectorGenHelper.buildPlanet(system, star,
            EntityLayout(OunalashkaIds.AMAKNAK, 185f, 180f, 3600f)
        )
        amaknak.customDescriptionId = OunalashkaIds.AMAKNAK.id + DESC
        Misc.initConditionMarket(amaknak)
        amaknak.market.addCondition(Conditions.RUINS_EXTENSIVE)
        amaknak.market.getFirstCondition(Conditions.RUINS_EXTENSIVE).isSurveyed = true
        amaknak.market.addCondition(Conditions.ORE_MODERATE)
        amaknak.market.addCondition(Conditions.RARE_ORE_SPARSE)
        amaknak.market.addCondition(Conditions.THIN_ATMOSPHERE)
        amaknak.market.addCondition(Conditions.POLLUTION)
        amaknak.market.addCondition(Conditions.HOT)

        val indieStation = SectorGenHelper.buildCustomEntity(system, amaknak,
            EntityLayout(OunalashkaIds.DUTCH_HARBOR, angle = 176f, orbitRadius = amaknak.radius + 150f, orbitDays = 30f)
        )
        indieStation.customDescriptionId = OunalashkaIds.DUTCH_HARBOR.id + DESC
        indieStation.addTag(Tags.STATION)

        SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(
                OunalashkaIds.COMM_RELAY, angle = SectorGenHelper.getTrailingLagrangeL5(amaknak.circularOrbitAngle),
                orbitRadius = amaknak.circularOrbitRadius, orbitDays = amaknak.circularOrbitPeriod
            )
        )
    }

    private fun buildOglodakSystem(system: StarSystemAPI, star: PlanetAPI) {
        val oglodak = SectorGenHelper.buildPlanet(system, star,
            EntityLayout(OunalashkaIds.OGLODAK, 250f, -20f, 6120f)
        )
        SectorGenHelper.buildRing(system, oglodak,
            RingLayout(Rings.ICE_0, 3, 400f, 22f)
        )
        SectorGenHelper.buildMagneticRing(system, oglodak,
            MagneticFieldLayout(oglodak.radius + 425f, oglodak.radius + 675f),
            auroraFreq = 0.5f
        )
        SectorGenHelper.buildPlanet(system, oglodak,
            EntityLayout(OunalashkaIds.ROUND, 70f, 30f, 1025f)
        )
        SectorGenHelper.buildInSystemJumpPoint(system, star,
            EntityLayout(
                OunalashkaIds.JP, angle = SectorGenHelper.getLeadingLagrangeL4(oglodak.circularOrbitAngle),
                orbitRadius = oglodak.circularOrbitRadius, orbitDays = oglodak.circularOrbitPeriod
            )
        )
        SectorGenHelper.buildAsteroidField(system, star,
            FieldLayout(
                ExternalStrings.OGLODAK_TROJANS, 900f, 900f,
                SectorGenHelper.getTrailingLagrangeL5(oglodak.circularOrbitAngle),
                oglodak.circularOrbitRadius,
                oglodak.circularOrbitPeriod
            ), 20, 30, 4f, 16f
        )
    }

    private fun buildOuterSystem(system: StarSystemAPI, star: PlanetAPI) {
        val radiusAfter = StarSystemGenerator.addOrbitingEntities(
            system, star, StarAge.YOUNG,
            3, 5,
            8260f,
            2,
            true
        )
        SectorGenHelper.buildCustomEntity(system, star,
            EntityLayout(OunalashkaIds.GATE, angle = 20f, orbitRadius = radiusAfter + 500f)
        )
    }
}