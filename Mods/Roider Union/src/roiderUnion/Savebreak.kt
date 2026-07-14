package roiderUnion

import com.fs.starfarer.api.EveryFrameScript
import com.thoughtworks.xstream.XStream
import roiderUnion.ids.Aliases

/**
 * Author: SafariJohn
 */
class Savebreak : EveryFrameScript {
    companion object {
        fun alias(x: XStream) {
            x.alias(Aliases.SAVEBREAK + ModPlugin.SAVE_BREAK_VERSION, Savebreak::class.java)
        }
    }

    override fun isDone(): Boolean { return false }
    override fun runWhilePaused(): Boolean { return false }
    override fun advance(amount: Float) {}
}