package scripts.campaign.ai

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.lwjgl.util.vector.Vector2f
import roiderUnion.helpers.ExternalStrings
import roiderUnion.ids.RoiderFleetTypes
import scripts.campaign.fleets.Roider_MinerManager

/**
 * Author: SafariJohn
 */
class Roider_MinerAssignmentAI(
    fleet: CampaignFleetAPI, source: MarketAPI,
    dest: SectorEntityToken, supplyLevels: MutableMap<String, Int>
) : EveryFrameScript {
    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "fleet", "f")
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "source", "s")
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "dest", "d")
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "supplyLevels", "su")
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "orderedReturn", "rn")
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "orderedRetreat", "rt")
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "preparing", "p")
            x.aliasAttribute(Roider_MinerAssignmentAI::class.java, "mining", "m")
        }
    }

    private val fleet: CampaignFleetAPI
    private val source: MarketAPI
    private val dest: SectorEntityToken
    private val supplyLevels: MutableMap<String, Int>
    private var orderedReturn = false
    private var orderedRetreat = false
    private var preparing = true
    private var mining = true

    override fun isDone(): Boolean {
        return !fleet.isAlive
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    init {
        this.fleet = fleet
        this.source = source
        this.dest = dest
        this.supplyLevels = supplyLevels
        giveAssignments()
    }

    private fun giveAssignments() {
        var daysToOrbit = daysToOrbit * 0.25f
        if (daysToOrbit < 0.2f) {
            daysToOrbit = 0.2f
        }
        fleet.addAssignment(
            FleetAssignment.ORBIT_PASSIVE, source.primaryEntity,
            daysToOrbit, "preparing for an expedition"
        )
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, dest, 1000f, "travelling")
        val daysToMine = daysToMine
        val list = cargoList
        fleet.addAssignment(
            FleetAssignment.ORBIT_PASSIVE, dest, daysToMine,
            "mining $list"
        )
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION, source.primaryEntity, 1000f,
            "returning to " + source.name + " with " + list
        )
        fleet.addAssignment(
            FleetAssignment.ORBIT_PASSIVE, source.primaryEntity, daysToOrbit * 2,
            "unloading $list"
        )
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source.primaryEntity, daysToOrbit,
            "returning to " + source.name
        )
    }

    private val daysToOrbit: Float
        get() {
            var daysToOrbit = 0f
            when (fleet.memoryWithoutUpdate.getString(MemFlags.MEMORY_KEY_FLEET_TYPE)) {
                RoiderFleetTypes.MINER -> daysToOrbit += 2f
                RoiderFleetTypes.MINING_FLEET -> daysToOrbit += 4f
                RoiderFleetTypes.MINING_ARMADA -> daysToOrbit += 6f
            }
            daysToOrbit *= (0.5f + Math.random().toFloat() * 0.5f)
            return daysToOrbit
        }
    private val daysToMine: Float
        get() {
            var daysToMine = 10f
            when (fleet.memoryWithoutUpdate.getString(MemFlags.MEMORY_KEY_FLEET_TYPE)) {
                RoiderFleetTypes.MINER -> daysToMine += 5f
                RoiderFleetTypes.MINING_FLEET -> daysToMine += 10f
                RoiderFleetTypes.MINING_ARMADA -> daysToMine += 15f
            }
            return daysToMine * (0.5f + Math.random().toFloat() * 0.5f)
        }

    private val cargoList: String
        get() {
            val strings: MutableList<String> = ArrayList()
            for (cid in supplyLevels.keys) {
                if (supplyLevels[cid]!! > 0) strings.add(Global.getSettings().getCommoditySpec(cid).lowerCaseName)
            }
            return if (strings.size > 1) Misc.getAndJoined(strings) else if (strings.size == 1) strings[0]
            else ExternalStrings.DEBUG_NULL // Should not happen
        }

    override fun advance(amount: Float) {
        if (fleet.ai.currentAssignment != null) {
            val fp: Float = fleet.fleetPoints.toFloat()
            val startingFP: Float =
                fleet.memoryWithoutUpdate.getFloat(Roider_MinerManager.MINER_STARTING_FP)
            if (fp < startingFP * 0.5f && !orderedReturn && !orderedRetreat) {
                orderedReturn = true
                preparing = false
                mining = false
                fleet.clearAssignments()
                if (source.primaryEntity != null) {
                    fleet.addAssignment(
                        FleetAssignment.GO_TO_LOCATION, source.primaryEntity, 1000f,
                        "returning to " + source.name // extern
                    )
                    fleet.addAssignment(
                        FleetAssignment.ORBIT_PASSIVE, source.primaryEntity, 1f,
                        "standing down"
                    )
                    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source.primaryEntity, 1000f)
                } else {
                    lost
                }
            } else {
                if (preparing && fleet.ai.currentAssignment.target === dest) {
                    preparing = false
                    loadHeavyMachinery()
                }
                if (!preparing && mining && fleet.ai.currentAssignment
                        .target === source.primaryEntity
                ) {
                    mining = false
                    loadResources()
                }
                if (source.primaryEntity == null && !orderedRetreat) {
                    orderedRetreat = true
                    fleet.clearAssignments()
                    lost
                }
            }
        } else {
            if (source.primaryEntity != null && !orderedReturn && !orderedRetreat) {
                // Don't think this case can actually happen.
                orderedReturn = true
                fleet.clearAssignments()
                if (source.primaryEntity != null) {
                    fleet.addAssignment(
                        FleetAssignment.GO_TO_LOCATION, source.primaryEntity, 1000f,
                        "returning to " + source.name // extern
                    )
                    fleet.addAssignment(
                        FleetAssignment.ORBIT_PASSIVE, source.primaryEntity, 1f,
                        "standing down"
                    )
                    fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source.primaryEntity, 1000f)
                }
            } else {
                orderedRetreat = true
                fleet.clearAssignments()
                lost
            }
        }
    }

    private fun loadHeavyMachinery() {
        val cargo: CargoAPI = fleet.cargo
        val maxCargo: Float = cargo.maxCapacity
        // 10% of max cargo for heavy machinery
        cargo.addCommodity(Commodities.HEAVY_MACHINERY, maxCargo * 0.1f)
        //        logger.info("asddssd heavy machinery loaded");
    }

    private fun loadResources() {
        var total = 0f
        for (cid in supplyLevels.keys) {
            val spec: CommoditySpecAPI = Global.getSettings().getCommoditySpec(cid)
            val qty: Float =
                (BaseIndustry.getSizeMult(supplyLevels[cid]!!.toFloat()) * spec.econUnit).toInt().toFloat()
            total += qty
            supplyLevels[cid] = qty.toInt()
        }
        if (total <= 0) return
        val cargo: CargoAPI = fleet.cargo
        var maxCargo: Float = cargo.maxCapacity
        maxCargo *= 0.9f // 10% taken up by heavy machinery
        for (cid in supplyLevels.keys) {
            val qty = supplyLevels[cid]!!.toFloat()
            cargo.addCommodity(cid, qty.toInt() * Math.min(1f, maxCargo / total))
            //            logger.info("asddssd " + cid + " loaded");
        }
    }

    // Add instant despawning code if player far away
    private val lost: Unit
        private get() {
            // Add instant despawning code if player far away
            val loc: Vector2f = Misc.getPointAtRadius(fleet.locationInHyperspace, 5000f)
            val token: SectorEntityToken = Global.getSector().hyperspace.createToken(loc)
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, token, 1000f, "travelling") // extern
        }
}