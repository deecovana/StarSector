package roiderUnion.combat

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import roiderUnion.helpers.Helper
import roiderUnion.helpers.LoopingListIterator
import roiderUnion.helpers.loopingIterator
import roiderUnion.hullmods.MIDAS
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.ids.ShipsAndWings
import java.util.WeakHashMap
import kotlin.math.max

class TrackerSpeedBoost : EveryFrameCombatPlugin {
    companion object {
        const val SPEED_BOOST = 20f
        const val MAX_SPEED = 220f
        private const val FRAMES_PER_LOOP = 150
    }
    private val engine: CombatEngineAPI
        get() = Helper.combatEngine ?: throw NullPointerException("Combat engine is null!")
    private lateinit var shipIterator: LoopingListIterator<ShipAPI?>

    private val trackers = WeakHashMap<ShipAPI, ShipAPI>()

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if (engine.isPaused || engine.ships.isEmpty()) return
        if (!this::shipIterator.isInitialized) shipIterator = engine.ships?.loopingIterator() ?: return

        val indicesPerIter = max(1, engine.ships.size / FRAMES_PER_LOOP)
        for (i in 0 ..< indicesPerIter) collectAndBoost()
    }

    private fun collectAndBoost() {
        if (shipIterator.nextIndex() == (engine.ships?.size ?: 0)) cleanupDeadTrackers()
        if (!this::shipIterator.isInitialized || !shipIterator.hasNext()) return
        val ship = shipIterator.next() ?: return

        if (ship.isFighter) {
            val isMIDASTracker = ship.isAlive && hasTrackerCore(ship.variant)
                    && MIDAS.hasMIDASStatic(ship.wing?.sourceShip?.variant)
            if (isMIDASTracker) {
                trackers[ship] = ship.wing?.sourceShip ?: return
            } else {
                val speedBoost = SPEED_BOOST.coerceAtMost(MAX_SPEED - (ship.mutableStats?.maxSpeed?.baseValue ?: 0f))
                if (trackers.values.contains(ship.wing?.sourceShip) && speedBoost > 0) {
                    ship.mutableStats?.maxSpeed?.modifyFlat(ShipsAndWings.TRACKER_WING, speedBoost)
                } else {
                    ship.mutableStats?.maxSpeed?.unmodifyFlat(ShipsAndWings.TRACKER_WING)
                }
            }
        }
    }

    private fun cleanupDeadTrackers() = trackers.keys.toList().filterNot { it.isAlive }.forEach { trackers.remove(it) }

    private fun hasTrackerCore(variant: ShipVariantAPI?): Boolean {
        return variant?.hasHullMod(RoiderHullmods.TRACKER_CORE) == true
    }

    override fun init(engine: CombatEngineAPI?) {}
    override fun processInputPreCoreControls(amount: Float, events: MutableList<InputEventAPI>?) {}
    override fun renderInWorldCoords(viewport: ViewportAPI?) {}
    override fun renderInUICoords(viewport: ViewportAPI?) {}
}