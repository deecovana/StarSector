package roiderUnion.nomads.bases

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.Memory
import roiderUnion.helpers.Settings
import roiderUnion.ids.Aliases
import roiderUnion.ids.MemoryKeys
import roiderUnion.nomads.NomadsHelper

class NomadBaseSpawnScript : EveryFrameScript {
    companion object {
        val MAX_BASES = Settings.MAX_NOMAD_BASES
        val FACTIONS = Settings.NOMAD_BASE_FACTION_WEIGHTS
        val MIN_INTERVAL = Settings.NOMAD_BASE_INTERVAL_MIN
        val MAX_INTERVAL = Settings.NOMAD_BASE_INTERVAL_MAX
        const val RETROFIT_STOCK_PERCENT = 0.6f

        fun alias(x: XStream) {
            val jClass = NomadBaseSpawnScript::class.java
            x.alias(Aliases.NBSPAWN, jClass)
            x.aliasAttribute(jClass, "interval", "i")
        }
    }

    private val interval = IntervalUtil(MIN_INTERVAL, MAX_INTERVAL)

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean  = false

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (!interval.intervalElapsed()) return
        if (NomadsHelper.bases.size >= MAX_BASES) return
        val system = NomadsHelper.pickSystemForRoiderBase() ?: return
        val faction = NomadsHelper.pickFaction()
        val base = NomadBaseBuilder.build(system, faction, NomadBaseLevel.STARTING) ?: return
        val nomads = NomadsHelper.pickInactiveOrNewNomadGroup()
        NomadsHelper.activeGroups += nomads
        Memory.set(MemoryKeys.NOMAD_GROUP, nomads, base)
        NomadsHelper.bases += base
    }
}