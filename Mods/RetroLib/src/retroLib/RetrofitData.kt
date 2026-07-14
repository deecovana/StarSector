package retroLib

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.thoughtworks.xstream.XStream
import java.lang.Exception

class RetrofitData(
    val id: String,
    val source: String,
    val target: String,
    val tags: Set<String>,
    val cost: Double? = Helper.calculateCost(source, target, null),
    val time: Double = 0.0,
    val reputation: RepLevel = RepLevel.VENGEFUL,
    val commission: Boolean = false
) {
    constructor(
        data: RetrofitData,
        id: String = data.id,
        source: String = data.source,
        target: String = data.target,
        tags: Set<String> = data.tags.toSet(),
        cost: Double? = data.cost,
        time: Double = data.time,
        reputation: RepLevel = data.reputation,
        commission: Boolean = data.commission
    ) : this(
        id,
        source,
        target,
        tags.toSet(),
        cost,
        time,
        reputation,
        commission
    )

    companion object {
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(RetrofitData::class.java, "id", "i")
            x.aliasAttribute(RetrofitData::class.java, "tags", "t")
            x.aliasAttribute(RetrofitData::class.java, "source", "s")
            x.aliasAttribute(RetrofitData::class.java, "target", "tr")
            x.aliasAttribute(RetrofitData::class.java, "cost", "c")
            x.aliasAttribute(RetrofitData::class.java, "time", "tm")
            x.aliasAttribute(RetrofitData::class.java, "reputation", "r")
            x.aliasAttribute(RetrofitData::class.java, "commission", "com")
        }
    }

    @Transient
    private var sourceSpecProperty: ShipHullSpecAPI? = null
    val sourceSpec: ShipHullSpecAPI?
        get() {
            if (sourceSpecProperty != null) return sourceSpecProperty!!
            val result = try {
                Helper.settings?.getHullSpec(source)
            } catch (ex: RuntimeException) {
                null
            }
            sourceSpecProperty = result
            return result
        }

    @Transient
    private var targetSpecProperty: ShipHullSpecAPI? = null
    val targetSpec: ShipHullSpecAPI
        get() {
            if (targetSpecProperty != null) return targetSpecProperty!!
            val result = try {
                Helper.settings?.getHullSpec(target)!!
            } catch (ex: Exception) {
                targetWingSpec!!.variant.hullSpec!!
            }
            targetSpecProperty = result
            return result
        }

    @Transient
    private var sourceWingSpecProperty: FighterWingSpecAPI? = null
    val sourceWingSpec: FighterWingSpecAPI?
        get() {
            if (sourceWingSpecProperty != null) return sourceWingSpecProperty!!
            val result = try {
                Helper.settings?.getFighterWingSpec(source)
            } catch (ex: RuntimeException) {
                null
            }
            sourceWingSpecProperty = result
            return result
        }

    @Transient
    private var targetWingSpecProperty: FighterWingSpecAPI? = null
    val targetWingSpec: FighterWingSpecAPI?
        get() {
            if (targetWingSpecProperty != null) return targetWingSpecProperty!!
            val result = Helper.settings?.getFighterWingSpec(target)
            targetWingSpecProperty = result
            return result
        }

    fun isLegal(faction: FactionAPI?): Boolean {
        if (faction == null || faction.isPlayerFaction) return true
        return isLegalRep(faction) && isLegalCom(faction)
    }

    fun isLegalRep(faction: FactionAPI): Boolean {
        val level = faction.getRelationshipLevel(Helper.sector?.playerFaction) ?: return false
        return level.isAtWorst(reputation)
    }

    fun isLegalCom(faction: FactionAPI): Boolean {
        return !commission || Helper.hasCommission(faction.id)
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