package roiderUnion.hullmods

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.BuffManagerAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.IntervalUtil
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.hullmods.RoiderHullmods
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PhasenetTowScript(val fleet: CampaignFleetAPI) : EveryFrameScript {
    companion object {
        const val HULLMOD_ID: String = RoiderHullmods.PHASENET_TOW_PATTERN
        fun getMaxBonusForSize(tugSize: ShipAPI.HullSize, size: ShipAPI.HullSize?): Int {
            if (size == null) return 4
            return ((tugSize.ordinal - size.ordinal) * 2).coerceAtLeast(1)
        }

        /**
         * One instance of the buff object per ship with a Phasenet.
         */
        const val ROIDER_PHASENET_TOW_KEY = "\$Roider_PhasenetTow_PersistentBuffs2"
    }

    class PhasenetTowPatternBuff(private val internalId: String) : BuffManagerAPI.Buff {
        private var bonus = 1
        var frames = 0f
        fun setBonus(bonus: Int) {
            this.bonus = bonus
        }

        override fun getId(): String = RoiderHullmods.PHASENET_TOW_PATTERN + "_$internalId"

        override fun isExpired(): Boolean = frames >= 1f

        override fun apply(member: FleetMemberAPI?) {
            member?.stats?.maxBurnLevel?.modifyFlat(id, bonus.toFloat())
        }

        override fun advance(days: Float) {
            frames += days
        }
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = true

    private val tugs = mutableSetOf<FleetMemberAPI>()
    private val playerInterval = IntervalUtil(0.4f, 0.6f)
    private val interval = IntervalUtil(4f, 6f)

    override fun advance(amount: Float) {
        if (fleet.isPlayerFleet) {
            playerInterval.advance(amount)
        } else if (Helper.sector?.isPaused == false) {
            interval.advance(amount)
        }
        if (!(playerInterval.intervalElapsed() || interval.intervalElapsed())) return

        val allShips = fleet.membersWithFightersCopy?.filterNot { it.isFighterWing }?.toMutableList() ?: return
        val tugShips = prepareTugs(allShips)

        sortFastestToSlowest(tugShips)
        sortFastestToSlowest(allShips)

        for (tug in tugShips) {
            applyExistingTowBuff(tug)
            val speedCutoff = tug.stats?.maxBurnLevel?.modifiedInt ?: continue
            val currentTarget = getCurrentTarget(tug, allShips)
            val newTarget = getSlowest(tug, allShips, speedCutoff, currentTarget) ?: continue
            val bonus = min(
                getMaxBonusForSize(tug.hullSpec.hullSize, newTarget.hullSpec?.hullSize),
                (speedCutoff - getMaxBurnWithoutPhasenet(newTarget)).toInt()
            )
            val buff = getPhasenetTowPatternBuffBy(tug, true)!!
            buff.setBonus(bonus)
            val existing = newTarget.buffManager.getBuff(buff.id)
            if (existing === buff) {
                buff.frames = 0f
            } else {
                buff.frames = 0f
                newTarget.buffManager.addBuff(buff)
            }
            if (currentTarget !== newTarget && currentTarget?.buffManager?.getBuff(buff.id) != null) {
                currentTarget.buffManager?.removeBuff(buff.id)
            }
        }
    }

    private fun prepareTugs(allShips: List<FleetMemberAPI>): MutableList<FleetMemberAPI> {
        for (tug in tugs.toList()) {
            if (!allShips.contains(tug)) {
                cleanUpPhasenetTowPatternBuffBy(tug)
                tugs.remove(tug)
            }
        }
        for (ship in allShips) {
            if (isTug(ship) && ship.canBeDeployedForCombat()) {
                tugs.add(ship)
            } else {
                cleanUpPhasenetTowPatternBuffBy(ship)
                tugs.remove(ship)
            }
        }
        return tugs.toMutableList()
    }

    private fun sortFastestToSlowest(ships: MutableList<FleetMemberAPI>) {
        ships.sortWith { o1, o2 ->
            (getMaxBurnWithoutPhasenet(o2) - getMaxBurnWithoutPhasenet(o1)).roundToInt()
        }
    }

    private fun applyExistingTowBuff(member: FleetMemberAPI) {
        for (b in member.buffManager?.buffs ?: emptyList()) {
            if (b.id.startsWith(HULLMOD_ID)) {
                b.apply(member)
                break
            }
        }
    }

    private fun getCurrentTarget(tug: FleetMemberAPI, allShips: List<FleetMemberAPI>): FleetMemberAPI? {
        return allShips.firstOrNull { hasCurrentTugMod(tug, it) }
    }

    private fun hasCurrentTugMod(tug: FleetMemberAPI, member: FleetMemberAPI): Boolean {
        return member.stats?.maxBurnLevel?.flatMods?.values
            ?.any { mod -> mod.source.startsWith(getTugPrefix(tug)) } == true
    }

    private fun getSlowest(
        tug: FleetMemberAPI,
        allShips: List<FleetMemberAPI>,
        speedCutoff: Int,
        currentTarget: FleetMemberAPI?,
    ): FleetMemberAPI? {
        var result: FleetMemberAPI? = null
        var lowestSpeed = Float.MAX_VALUE
        for (ship in allShips) {
            if (ship === tug) continue
            val burn = if (ship === currentTarget) getMaxBurnWithoutPhasenet(ship, tug) else ship.stats?.maxBurnLevel?.modifiedValue ?: continue
            if (burn >= speedCutoff) continue
            val boostedBurn = burn + getMaxBonusForSize(tug.hullSpec.hullSize, ship.hullSpec?.hullSize).coerceAtMost(speedCutoff)
            if (boostedBurn == lowestSpeed) {
                if (isTug(ship) && !isTug(result)) {
                    result = ship
                } else if (result?.isSmallerThan(ship) == true) {
                    result = ship
                }
            } else if (boostedBurn < lowestSpeed) {
                lowestSpeed = boostedBurn
                result = ship
            }
        }
        return result
    }

    private fun isTug(member: FleetMemberAPI?): Boolean {
        return member?.variant?.hasHullMod(HULLMOD_ID) ?: false
    }

    private fun getMaxBurnWithoutPhasenet(member: FleetMemberAPI): Float {
        return getMaxBurnWithoutPhasenet(HULLMOD_ID, member)
    }

    private fun getMaxBurnWithoutPhasenet(member: FleetMemberAPI, tug: FleetMemberAPI): Float {
        return getMaxBurnWithoutPhasenet(getTugPrefix(tug), member)
    }

    private fun getTugPrefix(tug: FleetMemberAPI): String = HULLMOD_ID + "_${tug.id}"

    private fun getMaxBurnWithoutPhasenet(prefix: String, member: FleetMemberAPI): Float {
        val burn: MutableStat = member.stats.maxBurnLevel ?: return 0f
        var sub = 0f
        for (mod in burn.flatMods.values) {
            if (mod.getSource()?.startsWith(prefix) == true) sub = mod.value; break
        }
        return max(0f, burn.modifiedValue - sub)

    }

    private fun FleetMemberAPI.isSmallerThan(m: FleetMemberAPI): Boolean = isSmaller(this, m)

    private fun isSmaller(m1: FleetMemberAPI, m2: FleetMemberAPI?): Boolean {
        if (m2?.hullSpec?.hullSize != null) return m1.hullSpec.hullSize.ordinal < m2.hullSpec.hullSize.ordinal
        var m1Size = 5
        var m2Size = 5
        when (m1.hullSpec.hullSize) {
            ShipAPI.HullSize.FRIGATE -> m1Size -= 4
            ShipAPI.HullSize.DESTROYER -> m1Size -= 3
            ShipAPI.HullSize.CRUISER -> m1Size -= 2
            ShipAPI.HullSize.CAPITAL_SHIP -> m1Size -= 1
            else -> {}
        }
        when (m2?.hullSpec?.hullSize) {
            ShipAPI.HullSize.FRIGATE -> m2Size -= 4
            ShipAPI.HullSize.DESTROYER -> m2Size -= 3
            ShipAPI.HullSize.CRUISER -> m2Size -= 2
            ShipAPI.HullSize.CAPITAL_SHIP -> m2Size -= 1
            else -> {}
        }
        return m1Size < m2Size
    }

    private fun cleanUpPhasenetTowPatternBuffBy(tug: FleetMemberAPI) {
        val buff = getPhasenetTowPatternBuffBy(tug, false)
        if (buff != null) {
            val buffs = Memory.get(
                ROIDER_PHASENET_TOW_KEY,
                { it is MutableMap<*,*> },
                { mutableMapOf<FleetMemberAPI, PhasenetTowPatternBuff>() }
            ) as MutableMap<FleetMemberAPI, PhasenetTowPatternBuff>
            buffs.remove(tug)
            tug.fleetData?.membersListCopy?.forEach { it.buffManager.removeBuff(buff.id) }
        }
    }

    private fun getPhasenetTowPatternBuffBy(
        tug: FleetMemberAPI,
        createIfMissing: Boolean
    ): PhasenetTowPatternBuff? {
        val buffs = Memory.get(
            ROIDER_PHASENET_TOW_KEY,
            { it is MutableMap<*,*> },
            { mutableMapOf<FleetMemberAPI, PhasenetTowPatternBuff>() }
        ) as MutableMap<FleetMemberAPI, PhasenetTowPatternBuff>

        var buff = buffs[tug]
        if (buff == null && createIfMissing) {
            buff = PhasenetTowPatternBuff(tug.id)
            buffs[tug] = buff
        }
        return buff
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PhasenetTowScript) return false
        return other.fleet === this.fleet
    }

    override fun hashCode(): Int {
        return fleet.hashCode()
    }
}