package roiderUnion.cleanup

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.ids.ShipsAndWings

/**
 * Author: SafariJohn
 */
class TrackerSingleBPAdderScript : EveryFrameScript {
    companion object {
        const val KNOWS_ARMATURE = "\$roider_knowsArmature"
        fun alias(x: XStream) {
            val jClass = TrackerSingleBPAdderScript::class.java
            x.alias(Aliases.ARMBPADDER, jClass)
            x.aliasAttribute(jClass, "tracker", "t")
        }
    }

    var tracker = IntervalUtil(0.33f, 0.5f)

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        tracker.advance(Misc.getDays(amount))
        if (!tracker.intervalElapsed()) return
        for (faction in Helper.sector?.allFactions ?: listOf()) {
            if (faction.memoryWithoutUpdate?.contains(KNOWS_ARMATURE) == true) continue
            val knowsWing = faction.knowsFighter(ShipsAndWings.TRACKER_WING)
            val knowsSingle = faction.knowsFighter(ShipsAndWings.TRACKER_SINGLE)
            if (knowsSingle && knowsWing) {
                faction.memoryWithoutUpdate[KNOWS_ARMATURE] = true
                continue
            }
            if (!knowsSingle && !knowsWing) continue
            if (!knowsWing && knowsSingle) faction.addKnownFighter(ShipsAndWings.TRACKER_WING, true)
            if (!knowsSingle && knowsWing) faction.addKnownFighter(ShipsAndWings.TRACKER_SINGLE, true)
        }
    }
}