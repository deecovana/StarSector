package roiderUnion.nomads.bases

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.Aliases
import roiderUnion.ids.MemoryKeys

class NomadBaseIntelInteractor(private val model: NomadBaseIntelModel, private val base: SectorEntityToken) {
    companion object {
        fun alias(x: XStream) {
            val jClass = NomadBaseIntelInteractor::class.java
            x.alias(Aliases.NBII, jClass)
            x.aliasAttribute(jClass, "model", "m")
            x.aliasAttribute(jClass, "base", "b")
            x.aliasAttribute(jClass, "addedListenerTo", "l")
        }

        const val TOKEN_FACTION = "\$faction"
        val DISCOVERED_PARAM = Object()
    }

    private var addedListenerTo: CampaignFleetAPI? = null

    init {
        model.base = base
    }

    fun isHidden(timestamp: Long?): Boolean = timestamp == null

    fun advance(amount: Float, intel: BaseIntelPlugin, listener: FleetEventListener) {
        if (intel.playerVisibleTimestamp == null && base.isInCurrentLocation && intel.isHidden) {
            makeKnown(intel)
            intel.sendUpdateIfPlayerHasIntel(DISCOVERED_PARAM, false)
        }

        val fleet = Misc.getStationFleet(base.market)
        if (fleet != null && addedListenerTo !== fleet) {
            if (addedListenerTo != null) {
                addedListenerTo!!.removeEventListener(listener)
            }
            fleet.addEventListener(listener)
            addedListenerTo = fleet
        }
    }

    private fun makeKnown(intel: BaseIntelPlugin) = makeKnown(intel, null)

    fun makeKnown(intel: BaseIntelPlugin, text: TextPanelAPI?) {
        if (intel.playerVisibleTimestamp == null) {
            Helper.sector?.intelManager?.removeIntel(intel)
            Helper.sector?.intelManager?.addIntel(intel, text == null, text)
        }
    }

    fun notifyEnding(intel: BaseIntelPlugin) {
        val location = base.containingLocation?.name ?: ExternalStrings.DEBUG_NULL
        NomadBaseIntelPlugin.log.info(String.format("Removing roider nomad base at [%s]", location))
        Helper.sector?.listenerManager?.removeListener(intel)
    }

    fun baseFleetDespawned(intel: BaseIntelPlugin) {
        intel.sendUpdateIfPlayerHasIntel(null, false)
    }

    fun getSortString(): String {
        return ExternalStrings.NOMAD_BASE_SORT_STRING.replace(TOKEN_FACTION, base.faction.personNamePrefix)
    }

    fun determineBaseState(intel: BaseIntelPlugin) {
        model.nomadBaseState = if (intel.isEnding) {
            NomadBaseState.ABANDONED
        } else if (intel.listInfoParam == DISCOVERED_PARAM) {
            NomadBaseState.DISCOVERED
        } else if (base.isDiscoverable) {
            NomadBaseState.LOC_UNKNOWN
        } else if (base.isExpired) {
            NomadBaseState.DESTROYED
        } else {
            NomadBaseState.KNOWN
        }
    }

    fun prepareSmallDescription(intel: BaseIntelPlugin) {
        model.nomadBaseTier = if (base.isDiscoverable) {
            null
        } else {
            Memory.getNullable(MemoryKeys.NOMAD_BASE_LEVEL, base, { it is NomadBaseLevel}, { null }) as? NomadBaseLevel
        }
        // optional base status (destroyed/rumored destroyed)
        determineBaseState(intel)
    }

    fun getIntelTags(): MutableSet<String> {
        val result = mutableSetOf<String>()
        result += Tags.INTEL_EXPLORATION
        result += base.faction.id
        return result
    }

    fun getMapLocation(): SectorEntityToken? {
        return if (base.isDiscoverable) {
            (base.containingLocation as? StarSystemAPI)?.center
        } else {
            base
        }
    }

}
