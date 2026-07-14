package retroLib.api

import com.fs.starfarer.api.fleet.FleetMemberAPI
import retroLib.RetrofitData
import retroLib.RetrofitDelivery

interface RetrofitDeliveryService {
    val isInstantOnly: Boolean
    val queued: MutableList<RetrofitDelivery>
    fun queue(source: FleetMemberAPI?, data: RetrofitData?)
    fun prioritize(sourceId: String)
    fun cancel(sourceId: String)
}