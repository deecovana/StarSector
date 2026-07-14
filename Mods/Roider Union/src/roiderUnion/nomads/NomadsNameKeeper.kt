package roiderUnion.nomads

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.MathUtils
import roiderUnion.helpers.Helper
import roiderUnion.helpers.ExternalStrings
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object NomadsNameKeeper {
    enum class Type {
        GROUP,
        BASE
    }
    enum class Suffix {
        NUMBER,
        NUMERAL,
        GREEK,
        NONE
    }
    class NomadsName(
        val type: Type,
        val prefix: String,
        val postfix: String,
        val suffixType: Suffix,
        val suffix: String
    ) {
        override fun toString(): String {
            return if (suffixType == Suffix.NONE) "$prefix $postfix"
            else "$prefix $postfix $suffix"
        }
    }

    private const val DEFAULT_NUMBER_CAP = 15
    private const val MAX_NUMERAL = 3999

    val usedNames = ArrayList<NomadsName>()

    fun generateName(type: Type): String {
        val prefix = pickPrefix(type)
        if (usedNames.any { it.prefix == prefix && it.type == type }) {
            return matchName(type, usedNames.first { it.prefix == prefix && it.type == type })
        }
        val postfix = pickPostfix(type)
        val suffixType = if (type == Type.GROUP) Suffix.NONE else pickSuffixType()
        val suffix = pickSuffix(suffixType, DEFAULT_NUMBER_CAP)

        val result = NomadsName(type, prefix, postfix, suffixType, suffix)
        usedNames.add(result)
        return result.toString()
    }

    private fun pickPrefix(type: Type): String {
        val picker = WeightedRandomPicker<String>(Helper.random)
        when (type) {
            Type.BASE -> picker.addAll(ExternalStrings.NOMAD_BASE_PREFIXES)
            Type.GROUP -> picker.addAll(ExternalStrings.NOMAD_GROUP_PREFIXES)
        }
        return picker.pick()
    }

    private fun pickPostfix(type: Type): String {
        val picker = WeightedRandomPicker<String>(Helper.random)
        when (type) {
            Type.BASE -> picker.addAll(ExternalStrings.NOMAD_BASE_POSTFIXES)
            Type.GROUP -> picker.addAll(ExternalStrings.NOMAD_GROUP_POSTFIXES)
        }
        return picker.pick()
    }

    private fun pickSuffixType(): Suffix {
        val picker = WeightedRandomPicker<Suffix>(Helper.random)
        picker.add(Suffix.NONE, 100f)
        picker.add(Suffix.NUMERAL, 10f)
        picker.add(Suffix.NUMBER, 10f)
//        picker.add(Suffix.GREEK, 1f)
        return picker.pick()
    }

    private fun pickSuffix(type: Suffix, cap: Int): String {
        if (type == Suffix.NONE) return ""

        val max = MathUtils.clamp(cap, 1, MAX_NUMERAL)
        val random = Helper.random
        val skipOneAndZero = 2
        return when (type) {
            Suffix.NUMBER -> (random.nextInt(max) + skipOneAndZero).toString()
            Suffix.NUMERAL -> Global.getSettings().getRoman(random.nextInt(max) + skipOneAndZero)
            Suffix.GREEK -> pickGreekLetter(random.nextInt(max) + skipOneAndZero)
            else -> ""
        }
    }

    private fun pickGreekLetter(test: Int): String {
        Global.getSettings().greekLetterReset()
        var i = test % 24
        while (i > 0) {
            i--
            Global.getSettings().getNextGreekLetter(Suffix.GREEK)
        }
        return Misc.ucFirst(Global.getSettings().getNextGreekLetter(Suffix.GREEK))
    }

    private fun matchName(type: Type, name: NomadsName): String {
        val suffixType = if (type == Type.GROUP) Suffix.GREEK else matchSuffixType(name)
        val result = NomadsName(type, name.prefix, name.postfix, suffixType, pickMatchingSuffix(name, suffixType))
        usedNames.add(result)
        return result.toString()
    }

    private fun matchSuffixType(name: NomadsName): Suffix {
        return if (name.suffixType == Suffix.NONE) pickSuffixType()
        else name.suffixType
    }

    private fun pickMatchingSuffix(name: NomadsName, type: Suffix): String {
        if (name.suffixType != Suffix.NONE) {
            val inUseSuffixes = HashSet<String>()
            for (n in usedNames) {
                if (n.prefix == name.prefix && n.postfix == name.postfix) {
                    inUseSuffixes.add(n.suffix)
                }
            }

            var cap = DEFAULT_NUMBER_CAP
            var suffix = pickSuffix(type, cap)
            while (inUseSuffixes.contains(suffix)) {
                if (cap >= MAX_NUMERAL) return ""
                cap = (cap * 1.05f).toInt()
                suffix = pickSuffix(type, cap)
            }

            return suffix
        } else {
            return pickSuffix(type, DEFAULT_NUMBER_CAP)
        }
    }
}
