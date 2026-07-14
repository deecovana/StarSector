package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.campaign.CampaignUtils
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.RoiderTags
import kotlin.random.Random
import kotlin.random.asJavaRandom

class ProspectingSpawner(
    seed: Long,
    private val sourceFleet: CampaignFleetAPI,
    private val prospectingZone: SectorEntityToken
) : EveryFrameScript {
    companion object {
        const val SPAWN_SECONDS_PER_ROID = 0.1f
        const val DAYS_PER_SPAWN = 0.01f
        const val MAX_SPAWN_RANGE = 1000f
        const val MIN_ROID_VALUE_MULT = 0.7f
        const val MAX_ROID_VALUE_MULT = 1.3f
    }

    private var spawns = 7

    private val interval = IntervalUtil(DAYS_PER_SPAWN, DAYS_PER_SPAWN)

    private val poll = RoidMiningHelper.pollRoids(sourceFleet, prospectingZone)

    private val random = Random(seed)

    private val isPlayer = sourceFleet.isPlayerFleet

    private val spawnRange
        get() = MAX_SPAWN_RANGE / (spawns + 1)

    override fun isDone(): Boolean = spawns <= 0

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (!interval.intervalElapsed()) return
        spawns--
        // Increasing radius and proximity checks
        // X dist from last spawn
        // Y dist from any other roid
        val type = getType()
        if (type == RoidType.NULL) return
        val location = getSpawnLocation() ?: return

        // Time delay between each spawn
        // How long total?
        // How long between each

        // Not sure exactly how to spawn them in
        // For example, like T-On fleets? As sensor contacts? etc.

        // Also need to handle NPC spawn?

        // Player need to see where to go
        // Player should probably see which type of roid is where
        spawnRoid(type, location)
    }

    private fun getType(): RoidType {
        val picker = WeightedRandomPicker<RoidType>(random.asJavaRandom())
        for (type in poll.percents) {
            picker.add(type.key, type.value)
        }
        return picker.pick()
    }

    private fun getSpawnLocation(): Vector2f? {
        val sources = CampaignUtils.getNearbyEntitiesWithTag<SectorEntityToken>(
            prospectingZone,
            RoidMiningHelper.getProspectRange(sourceFleet),
            RoiderTags.PROSPECTING_ZONE
        ) + prospectingZone
        val picker = WeightedRandomPicker<SectorEntityToken?>(random.asJavaRandom())
        picker.addAll(sources)
        return picker.pick()?.location
    }

    private fun spawnRoid(type: RoidType, location: Vector2f) {
        if (type == RoidType.NULL) return
        val roid = RoidMiningHelper.createRoid(
            Helper.random,
            type,
            Helper.sector?.playerFleet?.containingLocation,
            null) ?: return
        Memory.set(RoidMiningHelper.VALUE_KEY, getRoidValue(type), roid)
        val xRandom = random.nextFloat() * 200f
        val yRandom = random.nextFloat() * 200f
        roid.setLocation(location.x + xRandom, location.y + yRandom)
        if (isPlayer) roid.setFaction(type.playerFaction)
        else roid.setFaction(Factions.NEUTRAL)
        roid.sensorProfile = 1f
        roid.isTransponderOn = true
        roid.addScript(RoidCutting(roid, sourceFleet))
        sourceFleet.containingLocation?.addEntity(roid)
        Misc.fadeIn(roid, 1f)
        val velocity = getInitialVelocity()
        roid.velocity.x = velocity.x
        roid.velocity.y = velocity.y
        if (RoidMiningHelper.isInTerrain(roid)) AsteroidBouncer.ROIDS += roid
    }

    private fun getRoidValue(type: RoidType): Float {
        return type.baseValue * (MIN_ROID_VALUE_MULT + (MAX_ROID_VALUE_MULT - MIN_ROID_VALUE_MULT) * random.nextFloat())
    }

    private fun getInitialVelocity(): Vector2f {
        val result = Misc.getUnitVectorAtDegreeAngle(random.nextFloat() * 306f)
        val speedPerBurn = Helper.settings?.speedPerBurnLevel ?: 20f // extern
        val speed = speedPerBurn * random.nextFloat() * 7f + 7f
        result.x *= speed
        result.y *= speed
        return result
    }
}