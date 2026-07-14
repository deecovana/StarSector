package com.fs.starfarer.api.impl.campaign.procgen

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.procgen.Constellation.ConstellationType
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.*
import com.fs.starfarer.api.util.Misc
import kotlin.math.roundToInt

/**
 * Author: SafariJohn
 */
class Roider_StarSystemGenerator(param: CustomConstellationParams) : StarSystemGenerator(param) {
        /**
         * This method correctly identifies nebula type instead of
         * randomly picking one like the base class does.
         * @param system
         * @param parentStar
         * @param age
         * @param min
         * @param max
         * @param startingRadius
         * @param nameOffset
         * @param withSpecialNames
         * @return
         */
        fun addOrbitingEntitiesRoider(
            system: StarSystemAPI, parentStar: SectorEntityToken?, age: StarAge,
            min: Int, max: Int, startingRadius: Float,
            nameOffset: Int, withSpecialNames: Boolean
        ): Float {
            val p = CustomConstellationParams(age)
            p.forceNebula =
                true // not sure why this is here; should avoid small nebula at lagrange points though (but is that desired?)
            val gen = Roider_StarSystemGenerator(p)
            gen.system = system
            gen.starData = Global.getSettings()
                .getSpec(StarGenDataSpec::class.java, system.star.spec.planetType, false) as StarGenDataSpec
            gen.starAge = age
            gen.constellationAge = age
            gen.starAgeData = Global.getSettings().getSpec(AgeGenDataSpec::class.java, age.name, true) as AgeGenDataSpec
            gen.star = system.star
            gen.systemType = system.type
            if (system.type == StarSystemType.NEBULA) {
                when (age) {
                    StarAge.YOUNG -> gen.nebulaType = NEBULA_BLUE
                    StarAge.OLD -> gen.nebulaType = NEBULA_AMBER
                    else -> gen.nebulaType = NEBULA_DEFAULT
                }
            } else {
                gen.nebulaType = NEBULA_NONE
            }
            gen.systemCenter = system.center
            var starData = gen.starData
            var planetData: PlanetGenDataSpec? = null
            var parentPlanet: PlanetAPI? = null
            if (parentStar is PlanetAPI) {
                val planet = parentStar
                if (planet.isStar) {
                    starData = Global.getSettings()
                        .getSpec(StarGenDataSpec::class.java, planet.spec.planetType, false) as StarGenDataSpec
                } else {
                    planetData = Global.getSettings()
                        .getSpec(PlanetGenDataSpec::class.java, planet.spec.planetType, false) as PlanetGenDataSpec
                    parentPlanet = planet
                }
            }
            var parentOrbitIndex = -1
            var startingOrbitIndex = 0
            val addingAroundStar = parentPlanet == null
            var r = 0f
            if (parentStar != null) {
                r = parentStar.radius
            }
            val approximateExtraRadiusPerOrbit = 400f
            if (addingAroundStar) {
                parentOrbitIndex = -1
                startingOrbitIndex =
                    ((startingRadius - r - STARTING_RADIUS_STAR_BASE - STARTING_RADIUS_STAR_RANGE * 0.5f) /
                            (BASE_INCR * 1.25f + approximateExtraRadiusPerOrbit)).toInt()
                if (startingOrbitIndex < 0) startingOrbitIndex = 0
            } else {
                var dist = 0f
                if (parentPlanet?.orbitFocus != null) {
                    dist = Misc.getDistance(parentPlanet.location, parentPlanet.orbitFocus.location)
                }
                parentOrbitIndex = ((dist - r - STARTING_RADIUS_STAR_BASE - STARTING_RADIUS_STAR_RANGE * 0.5f) /
                        (BASE_INCR * 1.25f + approximateExtraRadiusPerOrbit)).toInt()
                startingOrbitIndex = ((startingRadius - STARTING_RADIUS_MOON_BASE - STARTING_RADIUS_MOON_RANGE * 0.5f) /
                        (BASE_INCR_MOON * 1.25f)).toInt()
                if (parentOrbitIndex < 0) parentOrbitIndex = 0
                if (startingOrbitIndex < 0) startingOrbitIndex = 0
            }
            val context = GenContext(
                gen, system, gen.systemCenter, starData,
                parentPlanet, startingOrbitIndex, age.name, startingRadius, MAX_ORBIT_RADIUS,
                planetData?.category, parentOrbitIndex
            )
            val result = gen.addOrbitingEntities(
                context,
                getNormalRandom(min.toFloat(), max.toFloat()).roundToInt(),
                false,
                addingAroundStar,
                false,
                false
            )
            val c = Constellation(ConstellationType.NORMAL, age)
            c.getSystems().add(system)
            c.setLagrangeParentMap(gen.lagrangeParentMap)
            c.setAllEntitiesAdded(gen.allNameableEntitiesAdded)
            c.isLeavePickedNameUnused = true
            val namer = NameAssigner(c)
            if (withSpecialNames) {
                namer.setSpecialNamesProbability(1f)
            } else {
                namer.setSpecialNamesProbability(0f)
            }
            namer.setRenameSystem(false)
            namer.setStructuralNameOffset(nameOffset)
            namer.assignNames(null, null)
            for (entity in gen.allNameableEntitiesAdded.keys) {
                if (entity is PlanetAPI && entity.getMarket() != null) {
                    entity.getMarket().name = entity.getName()
                }
            }
            return result.orbitalWidth * 0.5f
        }

        fun addSystemwideNebulaRoider(system: StarSystemAPI, age: StarAge) {
            val p = CustomConstellationParams(age)
            p.forceNebula = true
            val gen = Roider_StarSystemGenerator(p)
            gen.system = system
            gen.starData = Global.getSettings()
                .getSpec(StarGenDataSpec::class.java, system.star.spec.planetType, false) as StarGenDataSpec
            gen.starAge = age
            gen.constellationAge = age
            gen.starAgeData = Global.getSettings().getSpec(AgeGenDataSpec::class.java, age.name, true) as AgeGenDataSpec
            gen.systemType = system.type
            if (system.type == StarSystemType.NEBULA) {
                if (age == StarAge.YOUNG) gen.nebulaType = NEBULA_BLUE else if (age == StarAge.OLD) gen.nebulaType =
                    NEBULA_AMBER else gen.nebulaType = NEBULA_DEFAULT
            } else {
                gen.nebulaType = NEBULA_NONE
            }
            gen.addSystemwideNebula()
            system.age = age
            system.setHasSystemwideNebula(true)
        }
}