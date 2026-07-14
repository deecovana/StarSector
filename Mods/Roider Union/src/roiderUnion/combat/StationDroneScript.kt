package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.helpers.LoopingListIterator
import roiderUnion.helpers.loopingIterator
import roiderUnion.ids.ShipsAndWings
import roiderUnion.ids.Variants
import kotlin.math.max

class StationDroneScript : EveryFrameCombatPlugin {
    companion object {
        const val ID = "roider_stationDroneScript"

        private const val FRAMES_PER_LOOP = 100
    }

    private lateinit var shipIterator: LoopingListIterator<ShipAPI>

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (!this::shipIterator.isInitialized) shipIterator = Helper.combatEngine?.ships?.loopingIterator() ?: return
        val stationDrones = Helper.combatEngine?.customData?.getOrPut(ID, { mutableListOf<ShipAPI>() }) as? MutableList<ShipAPI> ?: return

        if (Helper.combatEngine?.ships?.isNotEmpty() == true) {
            val indicesPerIter = max(1, (Helper.combatEngine?.ships?.size ?: 0) / FRAMES_PER_LOOP)
            for (i in 0..<indicesPerIter) {
                val ship = shipIterator.next()
                if (ship.hullSpec?.hullId == ShipsAndWings.STATION_DRONE_HIDDEN) {
                    if (ship.droneSource?.isAlive == false) {
                        Helper.combatEngine?.removeEntity(ship)
                        continue
                    }
                    if (ship.customData?.containsKey(ID) == true) continue
                    spawnDrone(ship, stationDrones)
                }
            }
        }

        for (drone in stationDrones.toList()) {
            advanceDrone(stationDrones, drone)
        }
    }

    private fun spawnDrone(ship: ShipAPI, stationDrones: MutableList<ShipAPI>) {
        ship.setCustomData(ID, true)
        ship.collisionClass = CollisionClass.NONE
        val fleetManager = Helper.combatEngine?.getFleetManager(ship.owner) ?: return
        val wasSuppressed: Boolean = fleetManager.isSuppressDeploymentMessages
        fleetManager.isSuppressDeploymentMessages = true
        val stationDrone = fleetManager.spawnShipOrWing(Variants.STATION_DRONE, ship.location, ship.facing) ?: return
        stationDrone.setAnimatedLaunch()
        stationDrone.collisionClass = CollisionClass.FIGHTER
        stationDrone.fleetMember?.captain?.setPersonality(Personalities.RECKLESS)
        stationDrone.setCustomData(ID, ship)
        stationDrones += stationDrone
        fleetManager.isSuppressDeploymentMessages = wasSuppressed
    }

    private fun advanceDrone(stationDrones: MutableList<ShipAPI>, drone: ShipAPI) {
        if (!drone.isAlive) {
            stationDrones.remove(drone)
            return
        }
        val invisiDrone = drone.customData?.get(ID) as? ShipAPI ?: return
        if (invisiDrone.droneSource?.isAlive == false || invisiDrone.droneSource?.location == null) {
            Helper.combatEngine?.applyDamage(drone, drone.location, 100000f, DamageType.ENERGY, 0f, true, false, invisiDrone)
            stationDrones.remove(drone)
            return
        }
        drone.isPullBackFighters = false
        drone.blockCommandForOneFrame(ShipCommand.PULL_BACK_FIGHTERS)
        drone.location.set(invisiDrone.location)
        drone.facing = invisiDrone.facing
        if (Misc.getDistance(drone.location, invisiDrone.droneSource.location) < invisiDrone.droneSource.collisionRadius * 1.5f) drone.velocity.set(0f,0f)
        else {
            if (drone.collisionClass == CollisionClass.FIGHTER) drone.collisionClass = CollisionClass.SHIP
            drone.velocity.set(invisiDrone.velocity)
        }
    }

    @Deprecated("")
    override fun init(engine: CombatEngineAPI?) {}
    override fun processInputPreCoreControls(amount: Float, events: MutableList<InputEventAPI>?) {}
    override fun renderInWorldCoords(viewport: ViewportAPI?) {}
    override fun renderInUICoords(viewport: ViewportAPI?) {}
}