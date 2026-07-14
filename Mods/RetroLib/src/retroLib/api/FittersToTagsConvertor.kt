package retroLib.api

interface FittersToTagsConvertor {
    fun convert(vararg fitters: String): Set<String>
}