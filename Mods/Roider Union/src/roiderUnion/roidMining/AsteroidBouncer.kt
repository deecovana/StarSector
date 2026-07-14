package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.loading.CampaignPingSpec
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import org.magiclib.terrain.MagicAsteroidBeltTerrainPlugin
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import java.awt.Color
import kotlin.math.absoluteValue

class AsteroidBouncer : EveryFrameScript {
    companion object {
        @JvmField
        val CIRCLE_TERRAINS = mutableListOf(
            "asteroid_field"
        )

        @JvmField
        val RING_TERRAINS = mutableListOf(
            "asteroid_belt",
            "ring"
        )

        const val ROIDS_KEY = "\$roider_roidRoids"
        const val FLEETS_KEY = "\$roider_roidFleets"

        val ROIDS: MutableSet<SectorEntityToken>
            get() = Memory.get(ROIDS_KEY, { it is MutableSet<*> }, { mutableSetOf<SectorEntityToken>() }) as MutableSet<SectorEntityToken>
        val FLEETS: MutableMap<CampaignFleetAPI, Float>
            get() = Memory.get(FLEETS_KEY, { it is MutableMap<*,*> }, { mutableMapOf<CampaignFleetAPI, Float>() }) as MutableMap<CampaignFleetAPI, Float>

        const val BOUNCE_GRACE_TIME_KEY = "\$roider_roidBounceGrace_"
        const val BOUNCE_GRACE_TIME = 0.1f
        const val BOUNCE_GRACE_DIST_MULT = 1.1f // should this exist?
        const val ASTEROIDS_PER_FRAME = 200

        const val ASTEROID_INDEX_KEY = "\$roider_roidBounceIndex"

        const val ROID_TERRAIN_MEMORY_KEY = "\$roider_roidTerrainMemory"
        const val ROID_LAST_TERRAIN_KEY = "\$roider_roidLastTerrain"

        fun playEffects(entity: SectorEntityToken, asteroid: SectorEntityToken, dealDamage: Boolean, terrainEffectMult: Float) {
            val test = Helper.sector?.playerFleet?.location ?: Vector2f()
            val dist = Misc.getDistance(test, entity.location)
            if (dist < HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE) {
                val volumeMult = 0.75f * (0.5f + 0.5f * terrainEffectMult)
                if (dealDamage) {
                    Helper.soundPlayer?.playSound("hit_heavy", 1f, volumeMult, entity.location, Misc.ZERO)
                } else {
                    Helper.soundPlayer?.playSound("hit_shield_heavy_gun", 1f, volumeMult, entity.location, Misc.ZERO)
                }
            }
            val angle = Misc.getUnitVector(asteroid.location, entity.location)
            angle.scale(asteroid.radius)
            val location = asteroid.location.plus(angle)
            playHitGlow(entity.containingLocation, location, asteroid.velocity, terrainEffectMult)
        }

        private fun playHitGlow(location: LocationAPI, loc: Vector2f, vel: Vector2f, terrainEffectMult: Float) {
            val glowSize = 100f + 100f * terrainEffectMult + 50f * Math.random().toFloat()
            val color = Color.RED.brighter()
            Misc.addHitGlow(location, loc, vel, glowSize, color)

        }
    }

    private fun getIndex(location: LocationAPI): Int {
        return Memory.get(ASTEROID_INDEX_KEY, location, { it is Int }, { 0 }) as Int
    }

    private fun getInTerrainMemory(roid: SectorEntityToken): MutableSet<String> {
        return Memory.get(ROID_TERRAIN_MEMORY_KEY, roid, { it is MutableSet<*> }, { mutableSetOf<String>() }) as MutableSet<String>
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        FLEETS.keys.toList().forEach {
            FLEETS[it] = FLEETS[it]!! - Misc.getDays(amount)
            if (FLEETS[it]!! <= 0f) FLEETS.remove(it)
        }
        ROIDS.toList().filter { it.isExpired }.forEach { ROIDS.remove(it) }
        FLEETS.toList().filter { it.first.isExpired }.forEach { FLEETS.remove(it.first) }
        showRingsIfPlayerDive(amount)
        advanceLocations()
        bounceBackErrantRoids()
    }

    private fun advanceLocations() {
        val locations = ROIDS.union(FLEETS.keys).map { it.containingLocation }.toSet()
        locations.forEach {
            var counter = ASTEROIDS_PER_FRAME.coerceAtMost(it.asteroids.size)
            while (counter > 0) {
                counter--
                val index = getIndex(it).takeIf { i -> i < it.asteroids.size } ?: 0
                Memory.set(ASTEROID_INDEX_KEY, index + 1, it)
                val asteroid = it.asteroids[index]
                if (ROIDS.contains(asteroid)) continue
                if (Misc.getAsteroidSource(asteroid) == null) continue
                checkForAsteroidBounce(asteroid)
            }
            it.terrainCopy.filter { terrain -> CIRCLE_TERRAINS.union(RING_TERRAINS).contains(terrain.type) }
                .forEach { terrain ->
                    ROIDS.filter { roid -> roid.containingLocation === it }
                        .forEach { roid ->
                            val tMem = getInTerrainMemory(roid)
                            if (terrain.plugin?.containsEntity(roid) == true) {
                                tMem.add(terrain.id)
                            } else {
                                if (tMem.contains(terrain.id) && tMem.size == 1) {
                                    Memory.set(ROID_LAST_TERRAIN_KEY, terrain.id, roid)
                                }
                                tMem.remove(terrain.id)
                            }
                        }
                }
        }
    }

    private fun bounceBackErrantRoids() {
        ROIDS.filter { getInTerrainMemory(it).isEmpty() }.forEach { roid ->
            val lastTerrainId = Memory.getNullable(ROID_LAST_TERRAIN_KEY, roid, { it is String }, { null }) as? String
            val lastTerrain = roid.containingLocation?.terrainCopy?.firstOrNull { it.id == lastTerrainId }
            if (lastTerrain != null) {
                if (CIRCLE_TERRAINS.contains(lastTerrain.type)) {
                    bounceRoidCircleEdge(roid, lastTerrain)
                } else if (RING_TERRAINS.contains(lastTerrain.type)) {
                    bounceRoidRingEdge(roid, lastTerrain)
                }
                Memory.unset(ROID_LAST_TERRAIN_KEY, roid)
            }
        }
    }

    private fun bounceRoidCircleEdge(roid: SectorEntityToken, lastTerrain: CampaignTerrainAPI) {
        val vector = Misc.getUnitVector(roid.location, lastTerrain.location)
        bounceRoid(roid, vector)
    }

    private fun bounceRoidRingEdge(roid: SectorEntityToken, lastTerrain: CampaignTerrainAPI) {
        val originLocation = lastTerrain.location
        val ringParams = (lastTerrain.plugin as? BaseRingTerrain)?.ringParams ?: return
        val roidDist = Misc.getDistance(roid.location, originLocation)

        val vector = if (roidDist < ringParams.middleRadius) {
            Misc.getUnitVector(originLocation, roid.location)
        } else {
            Misc.getUnitVector(roid.location, originLocation)
        }
        bounceRoid(roid, vector)
    }

    private var interval = 0f

    private fun showRingsIfPlayerDive(amount: Float) {
        // adjust interval when player gets close to timing out
        interval += amount
        if (interval < 0.2f) return // extern
        interval = 0f
        if (!FLEETS.keys.contains(Helper.sector?.playerFleet)) return
        val currLocation = Helper.sector?.currentLocation ?: return
        currLocation.asteroids.filterNot { ROIDS.contains(it) }
            .filterNot { Misc.getAsteroidSource(it) == null }
            .filter { Misc.getDistance(Helper.sector?.playerFleet, it) < HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE }
            .forEach {
                val color = Misc.getNegativeHighlightColor()
                val range = it.radius
                val custom = CampaignPingSpec().apply {
                    this.color = color
                    this.isUseFactionColor = false
                    this.width = 4f // extern
                    this.minRange = range
                    this.range = range
                    this.duration = .6f // extern
                    this.alphaMult = 1f
                    this.inFraction = 0.5f // extern
                    this.num = 1
                }
                Helper.sector?.addPing(it, custom)
            }
    }

    private fun checkForAsteroidBounce(asteroid: SectorEntityToken?) {
        if (asteroid == null) return
        val entities = ROIDS.union(FLEETS.keys)
        entities.filterNot { Memory.isFlag(BOUNCE_GRACE_TIME_KEY + it.id, asteroid) }
            .filter { it.containingLocation === asteroid.containingLocation }
            .filter { checkDistance(asteroid, it) }
            .forEach { asteroidBounce(it, asteroid) }
    }

    private fun checkDistance(asteroid: SectorEntityToken, entity: SectorEntityToken): Boolean {
        val dist = Misc.getDistance(asteroid, entity)
        val bounceRadius = (entity.radius + asteroid.radius) * BOUNCE_GRACE_DIST_MULT
        return dist <= bounceRadius
    }

    private fun asteroidBounce(entity: SectorEntityToken, asteroid: SectorEntityToken) {
        Memory.set(BOUNCE_GRACE_TIME_KEY + entity.id, true, BOUNCE_GRACE_TIME, asteroid)
        if (entity is CampaignFleetAPI) {
            bounceFleet(asteroid, entity, Misc.getAngleInDegrees(asteroid.location, entity.location))
        } else {
            playEffects(entity, asteroid, true, 1f)
            val impactVector = Misc.getUnitVector(asteroid.location, entity.location)
            bounceRoid(entity, impactVector)
        }
    }

    private fun bounceRoid(roid: SectorEntityToken, impactVector: Vector2f) {
        val result = Vector2f(
            roid.velocity.x + impactVector.x * roid.velocity.length() * 2f,
            roid.velocity.y + impactVector.y * roid.velocity.length() * 2f
        )
        if (result.length() > 0) result.scale(roid.velocity.length() / result.length())
        roid.velocity.x = result.x
        roid.velocity.y = result.y
    }

    private fun bounceFleet(asteroid: SectorEntityToken, fleet: CampaignFleetAPI, impactAngle: Float) {
        if (fleet.isInHyperspaceTransition) return

        val damageChanceMod: MutableStat = fleet.commanderStats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DAMAGE_CHANCE)
                ?: MutableStat(1f)

        val doDamage = Memory.isFlag(MagicAsteroidBeltTerrainPlugin.IMPACT_RECENT, fleet)
                && (Helper.random.nextFloat() * damageChanceMod.modifiedValue > 0.5f)

        val target = if (doDamage) getDamageTarget(fleet) else null
        val damageMult = if (target != null) {
            val damageMod: MutableStat = fleet.commanderStats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DAMAGE)
                    ?: MutableStat(1f)
            val memberDamageMod = target.stats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DAMAGE)
                    ?: MutableStat(1f)
            (fleet.currBurnLevel - Misc.getGoSlowBurnLevel(fleet)) * damageMod.modifiedValue * memberDamageMod.modifiedValue
        } else  {
            0f
        }

        fleet.addScript(FleetBounce(asteroid, fleet, target, damageMult, impactAngle))
    }

    private fun getDamageTarget(fleet: CampaignFleetAPI): FleetMemberAPI? {
        val targets = WeightedRandomPicker<FleetMemberAPI>()
        for (m in fleet.fleetData?.membersListCopy ?: emptyList()) {
            if (m == null) continue
            var w = 1f
            when (m.hullSpec?.hullSize) {
                HullSize.CAPITAL_SHIP -> w = 20f
                HullSize.CRUISER -> w = 10f
                HullSize.DESTROYER -> w = 5f
                HullSize.FRIGATE -> w = 1f
                else -> {}
            }
            val memberDamageChanceMod = m.stats?.dynamic?.getStat(MagicAsteroidBeltTerrainPlugin.IMPACT_DAMAGE_CHANCE)
                    ?: MutableStat(1f)
            w *= memberDamageChanceMod.modifiedValue
            targets.add(m, w)
        }
        return targets.pick()
    }

    // Need to show rings around asteroids if player is diving
    // Rapidly ping rings when dive is close to ending?
}