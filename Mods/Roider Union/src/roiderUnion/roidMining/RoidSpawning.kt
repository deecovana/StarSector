package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.StarSystemAPI

class RoidSpawning : EveryFrameScript {
    companion object {
        const val ROIDS_PER_DAY = 1f // More if more terrains? Yes. Needs to depend on area
        const val SPAWN_RADIUS_LY = 5f

        @JvmField
        val FIELD_TERRAINS = mutableListOf(
            "asteroid_field"
        )

        @JvmField
        val RING_TERRAINS = mutableListOf(
            "asteroid_belt",
            "ring"
        )

        @JvmField
        val NEBULA_TERRAINS = mutableListOf(
            "nebula"
        )
    }


    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        // Spawn roids
        // - in belts
        // - in fields
        // - in rings
        // - in nebula
    }

    private fun roidAreaInSystem(system: StarSystemAPI): Float {
        var result = 0f
        for (terrain in system.terrainCopy) {
            if (FIELD_TERRAINS.contains(terrain.type)) {
                // circle
            }
            if (RING_TERRAINS.contains(terrain.type)) {
                // ring
            }
            if (NEBULA_TERRAINS.contains(terrain.type)) {
                // blob
            }
        }
        return result
    }
}