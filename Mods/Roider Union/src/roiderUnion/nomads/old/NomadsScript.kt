package roiderUnion.nomads.old

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.TaskTimer
import roiderUnion.helpers.Settings
import roiderUnion.nomads.NomadsData
import roiderUnion.nomads.NomadsHelper

class NomadsScript : EveryFrameScript {
    companion object {
        const val DECEMBER = 12
        const val JUNE = 6

        const val MAX_MAIN_DELAY = 10f

        const val MIN_INTER_DELAY = 60f
        const val MAX_INTER_DELAY = 120f

        const val EXTRA_CUTOFF = 70f
        const val EXTRA_CHANCE = 0.33f
        const val MIN_EXTRA_DELAY = 110f
        const val MAX_EXTRA_DELAY = 130f

        const val ROUTE_SOURCE_ID = "roider_nomad"
        const val BASE_ORBIT_DAYS = 3f

        const val BASE_NUM_DIVIDER = 5
    }

    private val dayInterval = IntervalUtil(0.99f, 0.99f)
    private val random = Helper.random

    init {
        createBases()
//        startGatheringTimers()
//        startRandomGatheringTimers()
    }

    override fun advance(amount: Float) {
        dayInterval.advance(Misc.getDays(amount))
        if (dayInterval.intervalElapsed()) {
            val clock = Helper.sector?.clock ?: return
            if (clock.day <= 1 && (clock.month == DECEMBER || clock.month == JUNE)) {
                createBases()
//                startGatheringTimers()
//                startRandomGatheringTimers()
            }
        }
    }

    private fun createBases() {
        val max = Settings.MAX_NOMAD_BASES
        val current = NomadsHelper.bases.size
        if (max == current) return
        var newBases = (max - current) / BASE_NUM_DIVIDER

        while (newBases > 0) {
            newBases--
            val dest = pickDestination() ?: continue
            val base = NomadsHelper.createBase(dest, NomadsHelper.pickFaction()) ?: continue
            NomadsHelper.bases += base
            Memory.set(PirateBaseManager.RECENTLY_USED_FOR_BASE, true, dest.center)
//            Memory.set(MemoryKeys.NOMAD_BASE_UPGRADING, false, base)
            NomadsHelper.createBaseIntel(base)
        }
    }

    private fun pickDestination(): StarSystemAPI? {
        val destinations = HashSet<StarSystemAPI>()
        if (NomadsHelper.bases.size < Settings.MAX_NOMAD_BASES) {
            val baseLoc = NomadsHelper.pickSystemForRoiderBase()
            if (baseLoc != null) destinations.add(baseLoc)
        }
        val picker = WeightedRandomPicker<StarSystemAPI>(random)
        picker.addAll(destinations)
        return picker.pick()
    }

    private fun startGatheringTimers() {
        NomadsHelper.bases.forEach { startTimer(it, random.nextFloat() * MAX_MAIN_DELAY) }
    }

    private fun startRandomGatheringTimers() {
        for (base in NomadsHelper.bases) {
            if (random.nextBoolean()) {
                val time = Helper.getRandomFloat(MIN_INTER_DELAY, MAX_INTER_DELAY)
                startTimer(base, time)
                val doExtraGathering = time < EXTRA_CUTOFF && getChanceExtraGathering()
                if (doExtraGathering) startTimer(base, Helper.getRandomFloat(MIN_EXTRA_DELAY, MAX_EXTRA_DELAY))
            }
        }
    }

    private fun getChanceExtraGathering(): Boolean = random.nextFloat() < EXTRA_CHANCE

    private fun startTimer(base: SectorEntityToken, time: Float) {
        val nomads = pickNomads()
        NomadsHelper.activeGroups.add(nomads)
        nomads.location = NomadsHelper.pickMigrationSource()
        Helper.sector?.addScript(TaskTimer(GatheringTask(nomads, base), time))
    }

    private fun pickNomads(): NomadsData {
        if (NomadsHelper.groups.isEmpty()) return NomadsHelper.createNomads()

        val picker = WeightedRandomPicker<NomadsData>(random)
        picker.addAll(NomadsHelper.groups.filter { !NomadsHelper.activeGroups.contains(it) })
        picker.add(NomadsHelper.createNomads())
        return picker.pick()
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false
}