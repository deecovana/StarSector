package roiderUnion.retrofits.old.base

import com.fs.starfarer.api.campaign.RepLevel
import com.thoughtworks.xstream.XStream

class RetrofitData(
    val id: String,
    val fitter: String,
    val sourceHull: String,
    val targetHull: String,
    val cost: Double, // credits
    val time: Double, // days
    val reputation: RepLevel,
    val commission: Boolean
) {
    constructor(data: RetrofitData) : this(
        data.id,
        data.fitter,
        data.sourceHull,
        data.targetHull,
        data.cost,
        data.time,
        data.reputation,
        data.commission
    )

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(RetrofitData::class.java, "id", "i")
            x.aliasAttribute(RetrofitData::class.java, "fitter", "f")
            x.aliasAttribute(RetrofitData::class.java, "sourceHull", "s")
            x.aliasAttribute(RetrofitData::class.java, "targetHull", "tar")
            x.aliasAttribute(RetrofitData::class.java, "cost", "c")
            x.aliasAttribute(RetrofitData::class.java, "time", "t")
            x.aliasAttribute(RetrofitData::class.java, "reputation", "r")
            x.aliasAttribute(RetrofitData::class.java, "commission", "com")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return if (other is RetrofitData) {
            id == other.id
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}