package roiderUnion.econ

import com.thoughtworks.xstream.XStream
import roiderUnion.ids.Aliases

class DivesModel {
    companion object {
        fun alias(x: XStream) {
            val jClass = DivesModel::class.java
            x.alias(Aliases.DIVESM, jClass)
        }
    }

    @Transient
    var isHostile = false
    @Transient
    var canMine = true
    @Transient
    var isFunctional = true
    @Transient
    var isDivesBpKnown = false
    @Transient
    var isHqBpKnown = false
    @Transient
    var isUnionHQ = false
    @Transient
    var showPostDescription = true
    @Transient
    var isMilitary = true
    @Transient
    var defenseBonus = 0f
    @Transient
    var isFullConversions = true
    @Transient
    lateinit var image: String
    @Transient
    lateinit var nameForModifier: String
    @Transient
    lateinit var defaultUnavailableReason: String
    @Transient
    var marketName = ""
    @Transient
    var theMarketFaction = ""
    @Transient
    var resources = ""
    @Transient
    var drugsDeficit = ""
    @Transient
    var isDrugsDeficit = false
    @Transient
    var militiaEnabled = true
    @Transient
    var militiaStrength = 0f
}
