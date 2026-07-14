package roiderUnion.econ

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Stats
import roiderUnion.helpers.Memory
import roiderUnion.ids.MemoryKeys
import roiderUnion.ids.RoiderFactions
import roiderUnion.ids.RoiderIndustries

class NomadBaseIndustry : BaseIndustry() {
    companion object {
        const val DEFENSE_BONUS = 1000f
    }

    override fun apply() {
        super.apply(true)
        market.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.modifyFlat(modId, DEFENSE_BONUS, nameForModifier)
        checkAndSetRetrofitting()
    }

    private fun checkAndSetRetrofitting(override: Boolean? = null) {
        val enabled = override ?: isFunctional && !market.isPlayerOwned
                && !(market.factionId == RoiderFactions.ROIDER_UNION && market.hasFunctionalIndustry(RoiderIndustries.UNION_HQ))
        Memory.set(MemoryKeys.NOMAD_BASE_INDUSTRY, enabled, market)
    }

    override fun notifyDisrupted() {
        checkAndSetRetrofitting(false)
    }

    override fun disruptionFinished() {
        checkAndSetRetrofitting()
    }

    override fun unapply() {
        super.unapply()
        checkAndSetRetrofitting(false)
        market.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.unmodify(modId)
    }

    override fun isAvailableToBuild(): Boolean {
        return false
    }

    override fun showWhenUnavailable(): Boolean = false
}