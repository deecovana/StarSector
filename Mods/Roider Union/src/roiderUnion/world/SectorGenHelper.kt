package roiderUnion.world

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.ImportantPeopleAPI
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.procgen.*
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Memory
import roiderUnion.ids.*
import roiderUnion.nomads.NomadsData
import roiderUnion.nomads.NomadsHelper
import roiderUnion.nomads.NomadsLevel
import roiderUnion.nomads.NomadsNameKeeper
import roiderUnion.nomads.bases.NomadBaseBuilder
import roiderUnion.nomads.bases.NomadBaseLevel
import java.awt.Color
import java.util.*

object SectorGenHelper {
    private val logger = Global.getLogger(SectorGenHelper::class.java)

    const val ROCKPIPER_ORBIT_ANGLE = 145f

    const val TOKEN_PLANET = "\$planet"
    const val TOKEN_SYSTEM = "\$system"

    fun createCenteredOrbit(orbitFocus: SectorEntityToken, orbitDays: Float = 100f): OrbitAPI? {
        return Global.getFactory()?.createCircularOrbit(orbitFocus, 0f, 0f, orbitDays)
    }

    /**
     * @author Tartiflette
     */
    fun clearNearbyHyperspace(system: StarSystemAPI) {
        val plugin: HyperspaceTerrainPlugin = Misc.getHyperspaceTerrain().plugin as HyperspaceTerrainPlugin
        val editor = NebulaEditor(plugin)
        val minRadius: Float = plugin.tileSize * 2f
        val radius: Float = system.maxRadiusInHyperspace
        editor.clearArc(
            system.location.x,
            system.location.y,
            0f,
            radius + minRadius * 0.5f,
            0f,
            360f)
        editor.clearArc(system.location.x, system.location.y,
            0f,
            radius + minRadius,
            0f,
            360f,
            0.25f)
    }

    fun getGravWellName(system: StarSystemAPI, planet: PlanetAPI, isNebula: Boolean = false): String {
        return if (isNebula) ExternalStrings.NEBULA_GAS_GIANT_GRAV_WELL.replace(TOKEN_PLANET, planet.fullName)
        else ExternalStrings.GAS_GIANT_GRAV_WELL.replace(TOKEN_SYSTEM, system.baseName).replace(TOKEN_PLANET, planet.fullName)
    }

    fun createNebula(systemId: String, system: StarSystemAPI, age: StarAge): PlanetAPI {
        val finalAge = if (age == StarAge.ANY) StarAge.AVERAGE else age

        system.type = StarSystemGenerator.StarSystemType.NEBULA
        system.baseName = system.baseName
        var starTypeId = NebulaTypes.AVERAGE
        if (finalAge == StarAge.OLD) starTypeId = NebulaTypes.OLD
        if (finalAge == StarAge.YOUNG) starTypeId = NebulaTypes.YOUNG
        val star: PlanetAPI = system.initStar(systemId, starTypeId, 0f, 0f)
        logger.info("creating " + system.location)
        star.isSkipForJumpPointAutoGen = true
        star.addTag(Tags.AMBIENT_LS)

        // Set background
        val picker = WeightedRandomPicker<String>()
        if (finalAge == StarAge.OLD) {
            picker.add(Backgrounds.NEBULA_OLD, 10f)
        } else if (finalAge == StarAge.YOUNG) {
            picker.add(Backgrounds.NEBULA_YOUNG, 10f)
        } else {
            picker.add(Backgrounds.NEBULA_1, 10f)
            picker.add(Backgrounds.NEBULA_2, 10f)
        }
        system.backgroundTextureFilename = picker.pick()
        val starData: StarGenDataSpec = Global.getSettings()
            .getSpec(StarGenDataSpec::class.java, star.spec.planetType, false) as StarGenDataSpec
        val min: Color = starData.lightColorMin
        val max: Color = starData.lightColorMax
        val lightColor: Color = Misc.interpolateColor(min, max, Random().nextFloat())
        system.lightColor = lightColor
        return star
    }

    fun finalizeNebula(system: StarSystemAPI, center: PlanetAPI, age: StarAge, fringeJP: Boolean) {
        Misc.generatePlanetConditions(system, age)
        logger.info("finalizing " + system.location)

        // Remove star
        system.removeEntity(center)
        val coronaPlugin: StarCoronaTerrainPlugin? = Misc.getCoronaFor(center)
        if (coronaPlugin != null) {
            system.removeEntity(coronaPlugin.entity)
        }
        system.star = null
        val systemCenter: SectorEntityToken = system.initNonStarCenter()
        for (entity in system.allEntities) {
            if (entity.orbitFocus === center ||
                entity.orbitFocus === system.center
            ) {
                entity.orbit = null
            }
        }
        system.center = systemCenter
        system.autogenerateHyperspaceJumpPoints(true, fringeJP)
        system.star = center
        Roider_StarSystemGenerator(StarSystemGenerator.CustomConstellationParams(age)).addSystemwideNebulaRoider(system, age)
    }

    fun initStar(system: StarSystemAPI, layout: EntityLayout): PlanetAPI {
        return system.initStar(
            layout.id,
            layout.type,
            layout.radius,
            layout.coronaRadius
        )
    }

    fun initStar(
        system: StarSystemAPI,
        layout: EntityLayout,
        windBurnLevel: Float,
        flareProbability: Float,
        crLossMult: Float
    ): PlanetAPI {
        return system.initStar(
            layout.id,
            layout.type,
            layout.radius,
            layout.coronaRadius,
            windBurnLevel,
            flareProbability,
            crLossMult
        )
    }

    fun initSecondary(
        system: StarSystemAPI,
        star: PlanetAPI,
        layout: EntityLayout,
        windBurnLevel: Float,
        flareProbability: Float,
        crLossMult: Float
    ): PlanetAPI {
        val result = buildPlanet(system, star, layout)
        system.addCorona(result, layout.coronaRadius, windBurnLevel, flareProbability, crLossMult)
        system.secondary = result
        return result
    }

    fun buildPlanet(system: StarSystemAPI, orbitFocus: SectorEntityToken, layout: EntityLayout): PlanetAPI {
        return system.addPlanet(
            layout.id,
            orbitFocus,
            layout.name,
            layout.type,
            layout.angle,
            layout.radius,
            layout.orbitRadius,
            layout.orbitDays
        )
    }

    val DEFAULT_MAGNETIC_FIELD_COLORS = arrayOf(
        Color(140, 100, 235),
        Color(180, 110, 210),
        Color(150, 140, 190),
        Color(140, 190, 210),
        Color(90, 200, 170),
        Color(65, 230, 160),
        Color(20, 220, 70)
    )

    fun createNomadGroup(level: NomadsLevel): NomadsData {
        val result = NomadsData(NomadsNameKeeper.generateName(NomadsNameKeeper.Type.GROUP), "")
        result.level = level
        NomadsHelper.groups.add(result)
        return result
    }

    fun createNomadBase(level: NomadBaseLevel, faction: String? = null): SectorEntityToken? {
        val system = NomadsHelper.pickSystemForRoiderBase() ?: return null
        val factionId = faction ?: NomadsHelper.pickFaction()
        val base = NomadBaseBuilder.build(system, factionId, level) ?: return null
        val nomads = NomadsHelper.pickInactiveOrNewNomadGroup()
        NomadsHelper.activeGroups += nomads
        Memory.set(MemoryKeys.NOMAD_GROUP, nomads, base)
        NomadsHelper.bases.add(base)
        return base
    }

    fun buildCoveringMagneticField(
        system: StarSystemAPI,
        orbitFocus: SectorEntityToken,
        visualWidth: Float,
        orbitDays: Float,
        baseColor: Color = DEFAULT_MAGNETIC_FIELD_COLORS[0],
        auroraFreq: Float = 0f,
        vararg colors: Color?
    ): SectorEntityToken {
        val auroraColors = if (colors.isNotEmpty()) colors else DEFAULT_MAGNETIC_FIELD_COLORS
        val result = system.addTerrain(
            Terrain.MAGNETIC_FIELD,
            MagneticFieldTerrainPlugin.MagneticFieldParams(
                orbitFocus.radius + visualWidth,
                (orbitFocus.radius + visualWidth) / 2f,
                orbitFocus,
                orbitFocus.radius + 50f,
                orbitFocus.radius + 50f + visualWidth,
                baseColor,
                auroraFreq,
                *auroraColors
            )
        )
        result.setCircularOrbit(orbitFocus, 0f, 0f, orbitDays)
        return result
    }

    val DEFAULT_MAGNETIC_RING_COLORS = arrayOf(
        Color(50, 20, 100, 40),
        Color(50, 20, 110, 130),
        Color(150, 30, 120, 150),
        Color(200, 50, 130, 190),
        Color(250, 70, 150, 240),
        Color(200, 80, 130, 255),
        Color(75, 0, 160),
        Color(127, 0, 255)
    )

    fun buildMagneticRing(
        system: StarSystemAPI,
        orbitFocus: SectorEntityToken,
        layout: MagneticFieldLayout,
        baseColor: Color = DEFAULT_MAGNETIC_RING_COLORS[0],
        auroraFreq: Float = 0f,
        vararg colors: Color?
    ): SectorEntityToken {
        val auroraColors = if (colors.isNotEmpty()) colors else DEFAULT_MAGNETIC_RING_COLORS
        val result = system.addTerrain(
            Terrain.MAGNETIC_FIELD,
            MagneticFieldTerrainPlugin.MagneticFieldParams(
                layout.width,
                layout.middle,
                orbitFocus,
                layout.innerRadius,
                layout.outerRadius,
                baseColor,
                auroraFreq,
                *auroraColors
            )
        )
        result.setCircularOrbit(orbitFocus, 0f, 0f, layout.orbitDays)
        return result
    }

    fun getLeadingLagrangeL4(angle: Float): Float {
        return angle - 60f
    }

    fun getTrailingLagrangeL5(angle: Float): Float {
        return angle + 60f
    }

    fun calculateOrbitDays(orbitRadius: Float): Float {
        return orbitRadius / (20f + Helper.random.nextFloat() * 5f)
    }

    fun buildDecoRing(system: StarSystemAPI, orbitFocus: SectorEntityToken, layout: RingLayout): RingBandAPI {
        return system.addRingBand(
            orbitFocus,
            layout.category,
            layout.texture,
            layout.textureWidth,
            layout.index,
            layout.color,
            layout.width,
            layout.middle,
            layout.orbitDays
        )
    }

    fun buildAsteroidBelt(
        system: StarSystemAPI,
        orbitFocus: SectorEntityToken,
        layout: BeltLayout,
        numAsteroids: Int,
        name: String? = null
    ): SectorEntityToken {
        return system.addAsteroidBelt(
            orbitFocus,
            numAsteroids,
            layout.orbitRadius,
            layout.width,
            layout.minDays,
            layout.maxDays,
            Terrain.ASTEROID_BELT,
            name
        )
    }
    fun buildAsteroidField(
        system: StarSystemAPI,
        orbitFocus: SectorEntityToken,
        layout: FieldLayout,
        minAsteroids: Int,
        maxAsteroids: Int,
        minSize: Float,
        maxSize: Float
    ): SectorEntityToken {
        val result = system.addTerrain(
            Terrain.ASTEROID_FIELD,
            AsteroidFieldTerrainPlugin.AsteroidFieldParams(
                layout.minRadius,
                layout.maxRadius,
                minAsteroids,
                maxAsteroids,
                minSize,
                maxSize,
                layout.name
            )
        )
        result.setCircularOrbit(orbitFocus, layout.angle, layout.orbitRadus, layout.orbitDays)
        return result
    }

    fun buildCustomEntity(
        system: StarSystemAPI,
        orbitFocus: SectorEntityToken,
        layout: EntityLayout
    ): CustomCampaignEntityAPI {
        val result = system.addCustomEntity(layout.id, layout.name, layout.type, layout.faction)
        result.setCircularOrbit(orbitFocus, layout.angle, layout.orbitRadius, layout.orbitDays)
        return result
    }

    fun buildInSystemJumpPoint(system: StarSystemAPI, orbitFocus: SectorEntityToken, layout: EntityLayout): JumpPointAPI {
        val result: JumpPointAPI = Global.getFactory().createJumpPoint(layout.id, layout.name)
        result.setCircularOrbit(orbitFocus, layout.angle, layout.orbitRadius, layout.orbitDays)
        result.setStandardWormholeToHyperspaceVisual()
        system.addEntity(result)
        return result
    }

    fun buildOrbitingToken(system: StarSystemAPI, orbitFocus: SectorEntityToken, layout: EntityLayout): SectorEntityToken {
        val result = system.createToken(Vector2f())
        system.addEntity(result)
        result.setCircularOrbit(orbitFocus, layout.angle, layout.orbitRadius, layout.orbitDays)
        return result
    }

    fun buildRing(system: StarSystemAPI, orbitFocus: SectorEntityToken, layout: RingLayout): SectorEntityToken {
        return system.addRingBand(
            orbitFocus,
            layout.category,
            layout.texture,
            layout.textureWidth,
            layout.index,
            layout.color,
            layout.width,
            layout.middle,
            layout.orbitDays,
            Terrain.RING,
            null
        )
    }

    fun convertOrbitToPointingDown(entity: SectorEntityToken) {
        entity.setCircularOrbitPointingDown(
            entity.orbitFocus,
            entity.circularOrbitAngle,
            entity.circularOrbitRadius,
            entity.circularOrbitPeriod
        )
    }

    fun convertOrbitToSpin(entity: SectorEntityToken, minSpin: Float, maxSpin: Float = minSpin) {
        entity.setCircularOrbitWithSpin(
            entity.orbitFocus,
            entity.circularOrbitAngle,
            entity.circularOrbitRadius,
            entity.circularOrbitPeriod,
            minSpin,
            maxSpin
        )
    }

    fun interpolateSystemLight(star: PlanetAPI): Color {
        val starData = Global.getSettings()
            .getSpec(StarGenDataSpec::class.java, star.spec.planetType, true) as? StarGenDataSpec
            ?: return star.lightColor
        val min: Color = starData.lightColorMin ?: return star.lightColor
        val max: Color = starData.lightColorMax ?: return star.lightColor
        return Misc.interpolateColor(min, max, Helper.random.nextFloat())?: return star.lightColor
    }

    fun buildCoverRing(system: StarSystemAPI, orbitFocus: SectorEntityToken, params: BaseRingTerrain.RingParams): SectorEntityToken {
        val result = system.addTerrain(Terrain.RING, params)
        result.orbit = createCenteredOrbit(orbitFocus)
        return result
    }

    fun addVoidResources(system: StarSystemAPI, vararg resources: Pair<String, String>) {
        for (rez in resources) {
            Memory.get(
                MemoryKeys.VOID_RESOURCE + rez.first,
                system,
                { it is String },
                { rez.second }
            )
        }
    }

    fun genVoidResource(system: StarSystemAPI, rez: String, conditions: Map<String, Double>, canAdd: (StarSystemAPI) -> Boolean) {
        if (canAdd(system)) {
            val picker = WeightedRandomPicker<String>(Helper.random)
            conditions.forEach { picker.add(it.key, it.value.toFloat()) }
            Memory.get(
                MemoryKeys.VOID_RESOURCE + rez,
                system,
                { it is String },
                { picker.pick() }
            )
        } else {
            Memory.unset(MemoryKeys.VOID_RESOURCE + rez, system)
        }
    }

    fun hasVoidResources(system: StarSystemAPI): Boolean {
        return system.memoryWithoutUpdate.keys.any { it.startsWith(MemoryKeys.VOID_RESOURCE) }
    }

    fun hasOre(system: StarSystemAPI): Boolean {
        return system.terrainCopy?.any { it.plugin is AsteroidFieldTerrainPlugin || it.plugin is AsteroidBeltTerrainPlugin } == true
    }

    fun hasRareOre(system: StarSystemAPI): Boolean {
        return Helper.random.nextBoolean() || hasOre(system)
    }

    fun hasOrganics(system: StarSystemAPI): Boolean {
        return hasNebula(system) && (system.age == StarAge.OLD || Helper.random.nextBoolean())
    }

    fun hasVolatiles(system: StarSystemAPI): Boolean {
        return hasNebula(system) && (system.age == StarAge.YOUNG || Helper.random.nextBoolean())
    }

    fun hasNebula(system: StarSystemAPI): Boolean = system.terrainCopy?.any { it.plugin is NebulaTerrainPlugin } == true

    fun removeMilBaseCommander(market: MarketAPI) {
        val ip: ImportantPeopleAPI = Global.getSector().importantPeople
        for (person in market.peopleCopy) {
            if (person.postId == Ranks.POST_BASE_COMMANDER && person.faction === market.faction && ip.getData(person).checkedOutFor.contains(
                    "permanent_staff"
                )
            ) {
                market.commDirectory.removePerson(person)
                market.removePerson(person)
                ip.getData(person).location.market = null
                ip.returnPerson(person, "permanent_staff")
                ip.removePerson(person)
            }
        }
    }

    fun addDive(sector: SectorAPI, systemId: String, entityId: String, industryId: String) {
        val system = sector.getStarSystem(systemId) ?: return
        val entity = system.getEntityById(entityId) ?: return
        val market = entity.market ?: return
        if (!market.isPlanetConditionMarketOnly) market.addIndustry(industryId)
    }

    fun markClaimed(sector: SectorAPI, systemId: String, entityId: String) {
        val system = sector.getStarSystem(systemId)
        var entity: SectorEntityToken? = null
        if (system != null) entity = system.getEntityById(entityId)
        entity?.memoryWithoutUpdate?.set(MemoryKeys.CLAIMED, true)
    }

    fun markSystemClaimed(sector: SectorAPI, systemId: String) {
        val system = sector.getStarSystem(systemId)
        if (system != null) {
            for (planet in system.planets) {
                planet.memoryWithoutUpdate[MemoryKeys.CLAIMED] = true
            }
        }
    }
}