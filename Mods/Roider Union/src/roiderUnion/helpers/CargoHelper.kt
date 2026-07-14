package roiderUnion.helpers

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import kotlin.math.min

object CargoHelper {
    fun addCommodity(id: String, qty: Float, cargo: CargoAPI?) {
        if (cargo == null) return
        val com = Helper.settings?.getCommoditySpec(id) ?: return
        val avail = makeSpace(com.basePrice, qty, cargo)
        cargo.addCommodity(id, min(avail, qty))
    }

    fun makeSpace(incValue: Float, incQty: Float, cargo: CargoAPI?): Float {
        if (cargo == null) return 0f
        if (cargo.spaceLeft > incQty) return cargo.spaceLeft
        var needed = incQty - cargo.spaceLeft
        while (needed > 0) {
            var lowest: CargoStackAPI? = null
            cargo.stacksCopy.filterNot { it.isSpecialStack || it.isPersonnelStack || it.isFuelStack || incValue < it.baseValuePerUnit }
                .forEach {
                    if ((lowest?.baseValuePerUnit ?: Int.MAX_VALUE) > it.baseValuePerUnit) lowest = it
                }
            if (lowest != null) {
                if (needed > lowest!!.size) {
                    needed -= lowest!!.size
                    cargo.removeStack(lowest)
                } else {
                    needed = 0f
                    lowest!!.size = lowest!!.size - needed
                }
            } else {
                break
            }
        }
        return cargo.spaceLeft
    }

    fun makeSpaceSpecial(incValue: Float, incQty: Float, cargo: CargoAPI?): Float {
        if (cargo == null) return 0f
        if (cargo.spaceLeft > incQty) return cargo.spaceLeft
        var needed = incQty - cargo.spaceLeft
        while (needed > 0) {
            var lowest: CargoStackAPI? = null
            cargo.stacksCopy.filterNot { it.isPersonnelStack || it.isFuelStack || incValue < it.baseValuePerUnit }
                .forEach {
                    if ((lowest?.baseValuePerUnit ?: Int.MAX_VALUE) > it.baseValuePerUnit) lowest = it
                }
            if (lowest != null) {
                if (needed > lowest!!.size) {
                    needed -= lowest!!.size
                    cargo.removeStack(lowest)
                } else {
                    needed = 0f
                    lowest!!.size = lowest!!.size - needed
                }
            } else {
                break
            }
        }
        return cargo.spaceLeft
    }
}