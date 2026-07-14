package roiderUnion.helpers

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

object SkillsHelper {
    fun sicStandardTooltip(tooltip: TooltipMakerAPI?, text: String) {
        tooltip?.addPara(text, 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
    }
}