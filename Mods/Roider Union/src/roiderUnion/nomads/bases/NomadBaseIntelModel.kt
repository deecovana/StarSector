package roiderUnion.nomads.bases

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.Aliases

class NomadBaseIntelModel {
    lateinit var base: SectorEntityToken
    var nomadBaseState: NomadBaseState? = null
    var nomadBaseTier: NomadBaseLevel? = null

    companion object {
        fun alias(x: XStream) {
            val jClass = NomadBaseIntelModel::class.java
            x.alias(Aliases.NBIM, jClass)
            x.aliasAttribute(jClass, "base", "b")
            x.aliasAttribute(jClass, "nomadBaseState", "s")
            x.aliasAttribute(jClass, "nomadBaseTier", "t")
        }
    }
}
