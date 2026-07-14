package roiderUnion.helpers

import com.fs.starfarer.api.util.Misc
import java.awt.Color

class StringToken(
    val token: String,
    val text: String,
    val color: Color = Misc.getTextColor()
) {
    var index: Int = -1
}
