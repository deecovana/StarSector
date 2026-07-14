package roiderUnion.helpers

import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc

object Memory {
    fun contains(key: String, memory: MemoryAPI?): Boolean = memory?.contains(key) == true
    fun contains(key: String, location: LocationAPI?): Boolean = contains(key, location?.memoryWithoutUpdate)
    fun contains(key: String, market: MarketAPI?): Boolean = contains(key, market?.memoryWithoutUpdate)
    fun contains(key: String, entity: SectorEntityToken?): Boolean = contains(key, entity?.memoryWithoutUpdate)
    fun contains(key: String): Boolean = contains(key, Helper.sector?.memoryWithoutUpdate)

    fun isKeyBad(key: String): Boolean = !key.startsWith("$")

    fun get(key: String, memory: MemoryAPI?, test: (Any?) -> Boolean, producer: () -> Any): Any {
        if (isKeyBad(key)) return producer.invoke()
        if (memory == null) return producer.invoke()
        val t = memory.get(key)
        return if (test.invoke(t)) {
            t
        } else {
            val result = producer.invoke()
            memory.set(key, result)
            result
        }
    }
    fun get(key: String, location: LocationAPI?, test: (Any?) -> Boolean, producer: () -> Any): Any {
        return get(key, location?.memoryWithoutUpdate, test, producer)
    }
    fun get(key: String, market: MarketAPI?, test: (Any?) -> Boolean, producer: () -> Any): Any {
        return get(key, market?.memoryWithoutUpdate, test, producer)
    }
    fun get(key: String, entity: SectorEntityToken?, test: (Any?) -> Boolean, producer: () -> Any): Any {
        return get(key, entity?.memoryWithoutUpdate, test, producer)
    }
    fun get(key: String, test: (Any?) -> Boolean, producer: () -> Any): Any {
        return get(key, Helper.sector?.memoryWithoutUpdate, test, producer)
    }

    fun getNullable(key: String, memory: MemoryAPI?, test: (Any?) -> Boolean, producer: () -> Any?): Any? {
        if (isKeyBad(key)) return producer.invoke()
        if (memory == null) return producer.invoke()
        val t = memory.get(key)
        return if (test.invoke(t)) {
            t
        } else {
            val result = producer.invoke()
            if (result != null) memory.set(key, result)
            result
        }
    }
    fun getNullable(key: String, location: LocationAPI?, test: (Any?) -> Boolean, producer: () -> Any?): Any? {
        return getNullable(key, location?.memoryWithoutUpdate, test, producer)
    }
    fun getNullable(key: String, market: MarketAPI?, test: (Any?) -> Boolean, producer: () -> Any?): Any? {
        return getNullable(key, market?.memoryWithoutUpdate, test, producer)
    }
    fun getNullable(key: String, entity: SectorEntityToken?, test: (Any?) -> Boolean, producer: () -> Any?): Any? {
        return getNullable(key, entity?.memoryWithoutUpdate, test, producer)
    }
    fun getNullable(key: String, test: (Any?) -> Boolean, producer: () -> Any?): Any? {
        return getNullable(key, Helper.sector?.memoryWithoutUpdate, test, producer)
    }

    fun swap(key: String, memory: MemoryAPI?, newValue: Any, producer: () -> Any): Any {
        val result = get(key, memory, { true }, producer)
        set(key, newValue, memory)
        return result
    }
    fun swap(key: String, location: LocationAPI?, newValue: Any, producer: () -> Any): Any {
        return swap(key, location?.memoryWithoutUpdate, newValue, producer)
    }
    fun swap(key: String, market: MarketAPI?, newValue: Any, producer: () -> Any): Any {
        return swap(key, market?.memoryWithoutUpdate, newValue, producer)
    }
    fun swap(key: String, entity: SectorEntityToken?, newValue: Any, producer: () -> Any): Any {
        return swap(key, entity?.memoryWithoutUpdate, newValue, producer)
    }
    fun swap(key: String, newValue: Any, producer: () -> Any): Any {
        return swap(key, Helper.sector?.memoryWithoutUpdate, newValue, producer)
    }

    fun isFlag(key: String, memory: MemoryAPI?): Boolean {
        if (isKeyBad(key)) return false
        if (!contains(key, memory)) return false
        return memory?.getBoolean(key) == true
    }
    fun isFlag(key: String, location: LocationAPI?): Boolean {
        if (location?.memoryWithoutUpdate == null) return false
        return isFlag(key, location.memoryWithoutUpdate)
    }
    fun isFlag(key: String, market: MarketAPI?): Boolean {
        if (market?.memoryWithoutUpdate == null) return false
        return isFlag(key, market.memoryWithoutUpdate)
    }
    fun isFlag(key: String, entity: SectorEntityToken?): Boolean {
        if (entity?.memoryWithoutUpdate == null) return false
        return isFlag(key, entity.memoryWithoutUpdate)
    }
    fun isFlag(key: String): Boolean = isFlag(key, Helper.sector?.memoryWithoutUpdate)

    fun set(key: String, value: Any?, memory: MemoryAPI?) {
        if (isKeyBad(key)) return
        memory?.set(key, value)
    }
    fun set(key: String, value: Any?, location: LocationAPI?) = set(key, value, location?.memoryWithoutUpdate)
    fun set(key: String, value: Any?, market: MarketAPI?) = set(key, value, market?.memoryWithoutUpdate)
    fun set(key: String, value: Any?, entity: SectorEntityToken?) = set(key, value, entity?.memoryWithoutUpdate)
    fun set(key: String, value: Any?) = set(key, value, Helper.sector?.memoryWithoutUpdate)

    fun set(key: String, value: Any?, expire: Float, memory: MemoryAPI?) {
        if (isKeyBad(key)) return
        memory?.set(key, value, expire)
    }
    fun set(key: String, value: Any?, expire: Float, location: LocationAPI?) = set(key, value, expire, location?.memoryWithoutUpdate)
    fun set(key: String, value: Any?, expire: Float, market: MarketAPI?) = set(key, value, expire, market?.memoryWithoutUpdate)
    fun set(key: String, value: Any?, expire: Float, entity: SectorEntityToken?) = set(key, value, expire, entity?.memoryWithoutUpdate)
    fun set(key: String, value: Any?, expire: Float) = set(key, value, expire, Helper.sector?.memoryWithoutUpdate)

    fun unset(key: String, memory: MemoryAPI?) {
        if (isKeyBad(key)) return
        memory?.unset(key)
    }
    fun unset(key: String, location: LocationAPI?) = unset(key, location?.memoryWithoutUpdate)
    fun unset(key: String, market: MarketAPI?) = unset(key, market?.memoryWithoutUpdate)
    fun unset(key: String, entity: SectorEntityToken?) = unset(key, entity?.memoryWithoutUpdate)
    fun unset(key: String) = unset(key, Helper.sector?.memoryWithoutUpdate)

    fun setFlag(key: String, reason: String, memory: MemoryAPI?, expire: Float = Float.MAX_VALUE) {
        if (isKeyBad(key)) return
        if (memory == null) return
        Misc.setFlagWithReason(memory, key, reason, true, expire)
    }
    fun setFlag(key: String, reason: String, location: LocationAPI?, expire: Float = Float.MAX_VALUE) {
        setFlag(key, reason, location?.memoryWithoutUpdate, expire)
    }
    fun setFlag(key: String, reason: String, market: MarketAPI?, expire: Float = Float.MAX_VALUE) {
        setFlag(key, reason, market?.memoryWithoutUpdate, expire)
    }
    fun setFlag(key: String, reason: String, entity: SectorEntityToken?, expire: Float = Float.MAX_VALUE) {
        setFlag(key, reason, entity?.memoryWithoutUpdate, expire)
    }
    fun setFlag(key: String, reason: String, expire: Float = Float.MAX_VALUE) {
        setFlag(key, reason, Helper.sector?.memoryWithoutUpdate, expire)
    }

    fun unsetFlag(key: String, reason: String, memory: MemoryAPI?): Boolean {
        if (isKeyBad(key)) return false
        if (memory == null) return false
        return Misc.setFlagWithReason(memory, key, reason, false, 0f)
    }
    fun unsetFlag(key: String, reason: String, location: LocationAPI?): Boolean {
        return unsetFlag(key, reason, location?.memoryWithoutUpdate)
    }
    fun unsetFlag(key: String, reason: String, market: MarketAPI?): Boolean {
        return unsetFlag(key, reason, market?.memoryWithoutUpdate)
    }
    fun unsetFlag(key: String, reason: String, entity: SectorEntityToken?): Boolean {
        return unsetFlag(key, reason, entity?.memoryWithoutUpdate)
    }
    fun unsetFlag(key: String, reason: String): Boolean {
        return unsetFlag(key, reason, Helper.sector?.memoryWithoutUpdate)
    }
}