package roiderUnion.world

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.systems.AtkaIds
import roiderUnion.nomads.NomadsHelper
import roiderUnion.nomads.bases.NomadBaseBuilder
import roiderUnion.nomads.bases.NomadBaseLevel
import roiderUnion.nomads.bases.NomadBaseLevelTracker

class RoiderUnionReformer : EveryFrameScript {
    private val intervalUtil = IntervalUtil(1f, 1f)

    override fun advance(amount: Float) {
        intervalUtil.advance(Misc.getDays(amount))
        if (intervalUtil.intervalElapsed()) {
            if (isKorovinUnion()) return
            if (isNewCapitalUp()) return
            if (isNewCapitalAppointed()) return
            val isAppointed = appointNewCapital()
            if (!isAppointed) spawnNewCapitalBase()
        }
    }

    private fun isKorovinUnion(): Boolean {
        return Helper.sector?.getStarSystem(AtkaIds.SYSTEM_NAME)?.planets
            ?.firstOrNull { it.id == AtkaIds.KOROVIN.id }?.market?.faction?.id == RoiderFactions.ROIDER_UNION
    }

    private fun isNewCapitalUp(): Boolean {
        val allMarkets = Helper.sector?.economy?.marketsCopy ?: return false
        return allMarkets.any { Memory.isFlag(MemoryKeys.NOMAD_BASE_UNION_CAPITAL, it) }
    }

    private fun isNewCapitalAppointed(): Boolean {
        return NomadsHelper.bases.any { Memory.isFlag(MemoryKeys.NOMAD_BASE_UNION_CAPITAL, it.market) }
    }

    private fun appointNewCapital(): Boolean {
        val unionBases = NomadsHelper.bases.filter { it.faction.id == RoiderFactions.ROIDER_UNION }
        if (unionBases.isEmpty()) return false
        val best = getHighestTierBase(unionBases) ?: return false
        Memory.set(MemoryKeys.NOMAD_BASE_UNION_CAPITAL, true, best.market)
        return true
    }

    private fun getHighestTierBase(bases: List<SectorEntityToken>): SectorEntityToken? {
        return checkTier(bases, NomadBaseLevel.HQ)
            ?: checkTier(bases, NomadBaseLevel.SHIPWORKS)
            ?: checkTier(bases, NomadBaseLevel.BATTLESTATION)
            ?: checkTier(bases, NomadBaseLevel.ESTABLISHED)
            ?: checkTier(bases, NomadBaseLevel.STARTING)
    }

    private fun checkTier(bases: List<SectorEntityToken>, level: NomadBaseLevel): SectorEntityToken? {
        return bases.firstOrNull { NomadBaseLevelTracker.getLevel(it) == level }
    }

    private fun spawnNewCapitalBase() {
        val system = NomadsHelper.pickSystemForRoiderBase() ?: return
        val result = NomadBaseBuilder.build(system, RoiderFactions.ROIDER_UNION, NomadBaseLevel.BATTLESTATION)
        Memory.set(MemoryKeys.NOMAD_BASE_UNION_CAPITAL, true, result?.market)
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false
}