package roiderUnion.nomads

enum class NomadsLevel {
    MINOR {
        override fun next(): NomadsLevel = MAJOR
        override fun previous(): NomadsLevel = MINOR
    },
    MAJOR {
        override fun next(): NomadsLevel = MAJOR
        override fun previous(): NomadsLevel = MINOR
    },
    ALLIED {
        override fun next(): NomadsLevel = ELITE
        override fun previous(): NomadsLevel = MAJOR
    },
    ELITE {
        override fun next(): NomadsLevel = UNION
        override fun previous(): NomadsLevel = ALLIED
    },
    UNION {
        override fun next(): NomadsLevel = UNION
        override fun previous(): NomadsLevel = UNION
    };

    abstract fun next(): NomadsLevel
    abstract fun previous(): NomadsLevel
}