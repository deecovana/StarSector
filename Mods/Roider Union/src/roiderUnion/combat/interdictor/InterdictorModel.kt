package roiderUnion.combat.interdictor

import com.fs.starfarer.api.combat.ShipAPI

class InterdictorModel {
    var mainTarget: ShipAPI? = null
    val targets = mutableListOf<ShipAPI>()
    var canShowFloaty = false
    var infoState = InterdictorInfoState.DEFAULT
}