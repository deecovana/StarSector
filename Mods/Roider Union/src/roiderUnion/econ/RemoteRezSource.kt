package roiderUnion.econ

import com.thoughtworks.xstream.XStream
import roiderUnion.ids.Aliases

enum class RemoteRezSource {
    BASE,
    VOID,
    PLANET,
    SYSTEM;

    companion object {
        fun alias(x: XStream) {
            x.alias(Aliases.RRZ, RemoteRezSource::class.java)
        }
    }
}