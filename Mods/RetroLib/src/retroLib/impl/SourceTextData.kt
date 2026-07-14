package retroLib.impl

import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI

class SourceTextData(
    val sourceName: String,
    val isAllowed: Boolean,
    val isLegalRep: Boolean,
    val isLegalCom: Boolean,
    val repName: String,
    val cost: Int,
    val time: Int,
    val frameReturnSize: HullSize?,
    val spec: ShipHullSpecAPI? = null,
    val wingSpec: FighterWingSpecAPI? = null
) {
    val isLegal = isLegalRep && isLegalCom
}