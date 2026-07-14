package roiderUnion.econ

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys

class DivesSupplyManager : EveryFrameScript {
    companion object {
        const val MINING_CHECK_DAYS_MIN = 9f
        const val MINING_CHECK_DAYS_MAX = 11f
        private val miningBonus = mutableMapOf<String, String>()

        fun getMiningBonus(system: StarSystemAPI, com: String): String? {
            if (miningBonus.isEmpty()) update()
            return miningBonus[system.baseName + com]
        }

        private fun update() {
            for (s in Helper.sector?.starSystems ?: emptyList()) {
                getMiningBonusFromMemory(s, Commodities.ORE)
                getMiningBonusFromMemory(s, Commodities.RARE_ORE)
                getMiningBonusFromMemory(s, Commodities.ORGANICS)
                getMiningBonusFromMemory(s, Commodities.VOLATILES)
            }
        }

        private fun getMiningBonusFromMemory(system: StarSystemAPI, com: String) {
            val c = Memory.getNullable(MemoryKeys.VOID_RESOURCE + com, system, { it is String }, { null }) as String?
            if (c != null) miningBonus[system.baseName + com] = c
        }
    }

    private val interval = IntervalUtil(MINING_CHECK_DAYS_MIN, MINING_CHECK_DAYS_MAX)

    init {
        update()
    }

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (!interval.intervalElapsed()) return
        update()
    }
    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = false
}