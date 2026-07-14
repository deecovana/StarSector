package roiderUnion.nomads.old.bases

import roiderUnion.nomads.NomadsData

interface NomadBaseIntelAPI {
    fun isNomadBase(): Boolean
    fun arrival(group: NomadsData)
    fun departure()

}
