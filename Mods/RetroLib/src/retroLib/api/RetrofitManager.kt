package retroLib.api

import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.fleet.FleetMemberAPI
import retroLib.RetrofitData

interface RetrofitManager {
    val retrofits: List<RetrofitData>

    fun getAllTargets(): List<FleetMemberAPI>
    fun getAvailableTargets(): List<FleetMemberAPI>
    fun getUnavailableTargets(): List<FleetMemberAPI>
    fun getIllegalTargets(): List<FleetMemberAPI>
    fun getNotAllowedTargets(): List<FleetMemberAPI>
    fun getSourcesData(target: FleetMemberAPI?): List<RetrofitData>
    fun isSourceAllowed(data: RetrofitData): Boolean
    fun isSourceLegalRep(data: RetrofitData): Boolean
    fun isSourceLegalCom(data: RetrofitData): Boolean
    fun getAvailableSourceShips(target: FleetMemberAPI?): List<FleetMemberAPI>
    fun getQueuedData(queue: List<FleetMemberAPI>, target: FleetMemberAPI?): List<RetrofitData>
    fun isTargetAllowed(targetId: String?): Boolean
    fun isTargetLegal(targetId: String?): Boolean
    fun getTargetRep(targetId: String?): RepLevel
    fun isTargetCom(targetId: String?): Boolean
}