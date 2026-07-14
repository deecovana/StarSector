package scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import roiderUnion.ids.MemoryKeys
import roiderUnion.retrofits.ArgosRetrofitManager_Old
import roiderUnion.retrofits.ArgosRetrofitPlugin
import java.awt.Color

/**
 * Argos Paid Retrofitting
 * Author: SafariJohn
 */
class Roider_APRAccess : BaseCommandPlugin() {
    companion object {
        const val HAVE_CATALOG = "\$roider_aprHaveCatalog"
        const val COLUMNS = 7
    }

    private var memory: MemoryAPI? = null
    private var dialog: InteractionDialogAPI? = null
    private var entity: SectorEntityToken? = null
    private var person: PersonAPI? = null
    private var faction: FactionAPI? = null
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: List<Misc.Token>,
        memoryMap: Map<String, MemoryAPI>
    ): Boolean {
        this.dialog = dialog
        val command: String = params[0].getString(memoryMap) ?: return false
        memory = getEntityMemory(memoryMap)
        entity = dialog.interactionTarget
        person = entity?.activePerson
        if (person == null) return false
        faction = person!!.faction
        when (command) {
            "retrofit" -> retrofit()
            "catalog" -> showCatalogInit() // extern?
            "catalogCont" -> showCatalogCont()
        }
        return true
    }

    private fun retrofit() {
        // Get retrofit manager
        val manager: ArgosRetrofitManager_Old
        if (memory!!.get(MemoryKeys.RETROFITTER) != null) {
            manager = memory!!.get(MemoryKeys.RETROFITTER) as ArgosRetrofitManager_Old
        } else {
            manager = ArgosRetrofitManager_Old(
                entity!!,
                faction!!,
                memory!!.get(MemoryKeys.APR_OFFERINGS) as List<String>
            )
            memory!!.set(MemoryKeys.RETROFITTER, manager)
            Global.getSector().addScript(manager)
        }
        val plugin = ArgosRetrofitPlugin(
            dialog!!.plugin,
            manager,
            dialog!!.plugin.memoryMap
        )
        dialog!!.plugin = plugin
        plugin.init(dialog!!)
    }

    private fun showCatalogInit() {
        if (memory!!.getBoolean(HAVE_CATALOG)) {
            showCatalogCont()
            return
        }
        memory!!.set(HAVE_CATALOG, true)
        dialog!!.textPanel.addPara(
            Misc.ucFirst(person!!.rank)
                    + " " + person!!.nameString
                    + " transmits a retrofits catalog to you." // extern
        )
        showCatalog()
    }

    private fun showCatalogCont() {
        dialog!!.textPanel.addPara(
            "You still have the retrofits "
                    + "catalog that " + Misc.ucFirst(person!!.rank)
                    + " " + person!!.nameString
                    + " gave you." // extern
        )
        showCatalog()
    }

    private fun showCatalog() {
        // Get retrofit manager
        val manager: ArgosRetrofitManager_Old
        if (memory!!.get(MemoryKeys.RETROFITTER) != null) {
            manager = memory!!.get(MemoryKeys.RETROFITTER) as ArgosRetrofitManager_Old
        } else {
            manager = ArgosRetrofitManager_Old(
                entity!!,
                faction!!,
                memory!!.get(MemoryKeys.APR_OFFERINGS) as List<String>
            )
            memory!!.set(MemoryKeys.RETROFITTER, manager)
            Global.getSector().addScript(manager)
        }
        val retrofitHulls: CampaignFleetAPI = FleetFactoryV3.createEmptyFleet(
            manager.faction.id,
            FleetTypes.MERC_PRIVATEER, null
        )
        val included: MutableList<String> = ArrayList()
        for (data in manager.retrofits) {
            if (included.contains(data.targetHull)) continue
            retrofitHulls.fleetData.addFleetMember(data.targetHull + "_Hull")
            included.add(data.targetHull)
        }
        val retrofitMembers: List<FleetMemberAPI> = retrofitHulls.fleetData.membersListCopy

        // Match names
        for (m in retrofitMembers) {
            m.shipName = "Retrofit to" // extern
            m.repairTracker.cr = 0.7f // Looks cleaner
        }
        val members: MutableList<FleetMemberAPI> = ArrayList<FleetMemberAPI>()
        members.addAll(retrofitMembers)
        val targetTooltip: TooltipMakerAPI = dialog!!.textPanel.beginTooltip()
        targetTooltip.addTitle("Available Retrofits") // extern
        val rows = retrofitMembers.size / COLUMNS + 1
        val iconSize: Float = dialog!!.textWidth / COLUMNS
        val pad = 0f // 10f
        val color: Color = manager.faction.baseUIColor
        targetTooltip.addShipList(COLUMNS, rows, iconSize, color, members, pad)
        dialog!!.textPanel.addTooltip()
    }
}