package roiderUnion.roidMining

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidSource

class ProspectZoneSpawner : EveryFrameScript, AsteroidSource {
    override fun isDone(): Boolean {
        TODO("Not yet implemented")
    }

    override fun runWhilePaused(): Boolean {
        TODO("Not yet implemented")
    }

    override fun advance(amount: Float) {
        TODO("Not yet implemented")
    }

    override fun reportAsteroidPersisted(asteroid: SectorEntityToken?) {
        TODO("Not yet implemented")
    }

    override fun regenerateAsteroids() {
        TODO("Not yet implemented")
        // Do a varied grid spawn pattern, using a fleet to check terrains?
        // Or just make asteroids interactable? Not enough bc rings and nebula

        // Go through each terrain and calculate area, then place asteroids as appropriate?
    }
}