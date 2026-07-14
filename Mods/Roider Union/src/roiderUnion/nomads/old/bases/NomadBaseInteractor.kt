package roiderUnion.nomads.old.bases

import roiderUnion.nomads.NomadsData

class NomadBaseInteractor(private val model: NomadBaseModel) {
    var timestamp: Long? = null

    fun arrival(group: NomadsData) {
        model.group = group
    }

    fun departure() {

    }

    fun isHidden(hidden: Boolean): Boolean = hidden || timestamp == null
    fun isNomadBase(): Boolean = true

}
