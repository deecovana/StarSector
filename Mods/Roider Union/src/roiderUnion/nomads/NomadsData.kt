package roiderUnion.nomads

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.Aliases

class NomadsData(val name: String, val desc: String) {
    companion object {
        fun alias(x: XStream) {
            val jClass = NomadsData::class.java
            x.alias(Aliases.ND, jClass)
            x.aliasAttribute(jClass, "name", "n")
            x.aliasAttribute(jClass, "desc", "d")
            x.aliasAttribute(jClass, "level", "l")
            x.aliasAttribute(jClass, "knownBPs", "bps")
            x.aliasAttribute(jClass, "location", "loc")
            x.aliasAttribute(jClass, "remainingStay", "r")

        }
    }

    var level = NomadsLevel.MINOR
    val knownBPs = HashSet<String>()
    var location: SectorEntityToken? = null
    var remainingStay: Float = 0f
}
