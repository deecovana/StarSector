package roiderUnion.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Voices
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.Misc
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.Memory
import roiderUnion.ids.*
import roiderUnion.ids.hullmods.RoiderHullmods
import scripts.campaign.rulecmd.Roider_APRAccess

/**
 * Author: SafariJohn
 */
class Roider_MadRockpiper : HubMissionWithBarEvent() {
    companion object {
        const val ACTION_SHOW = "showPicker"
        const val ACTION_SP = "spentSP"
        const val ACTION_CAN_PICK = "canPick"

        const val OPTION_SHOW_PICKER = "roider_mRPShowPicker"

        const val TRIGGER_POST_TEXT = "Roider_MRPPostText"
        const val TRIGGER_PICKED = "Roider_MRPPicked"

        const val MRP_REF = "\$roider_madRockpiper_ref"
        const val MADMAN_KEY = "\$roider_madRockpiperData"
        const val EVENT_DAYS = 40f

        const val HIM_HER = "\$roider_mrp_himOrHer"
        const val HIM_HERSELF = "\$roider_mrp_himselfOrHerself"
        const val MADMAN_WOMAN = "\$roider_mrp_madmanOrWoman"
        const val IS_INHOSP = "\$roider_mrp_isInhosp"
    }

    private var sPRequired: Boolean
        get() = Memory.get(MemoryKeys.MRP_REQ_SP, { it is Boolean }, { false }) as Boolean
        set(required) { Memory.set(MemoryKeys.MRP_REQ_SP, required, EVENT_DAYS) }

    override fun create(createdAt: MarketAPI, barEvent: Boolean): Boolean {
        if (!barEvent) return false

        val isUnion = createdAt.factionId == RoiderFactions.ROIDER_UNION
        if (!(isUnion || Helper.hasRoiders(createdAt))) return false
        setGiverFaction(RoiderFactions.ROIDER_UNION)
        setGiverPost(Ranks.POST_CITIZEN)
        setGiverVoice(Voices.SPACER)
        findOrCreateGiver(createdAt, false, true)
        val person: PersonAPI = person
        setRepFactionChangesTiny()
        val refSet: Boolean = setPersonMissionRef(person, MRP_REF)
        setDoNotAutoAddPotentialContactsOnSuccess()

        if (!Memory.contains(MemoryKeys.MRP_REQ_SP) && genRandom.nextFloat() < 0.1f) sPRequired = true
        return refSet
    }

    override fun getPerson(): PersonAPI {
        var person: PersonAPI? = Global.getSector().memoryWithoutUpdate[MADMAN_KEY] as? PersonAPI
        if (person == null) {
            person = Global.getSector().getFaction(RoiderFactions.ROIDER_UNION).createRandomPerson()
            person.name = FullName(ExternalStrings.MRP_FIRST_NAME, ExternalStrings.MRP_LAST_NAME, person.gender)
            if (person.gender == FullName.Gender.MALE) {
                person.portraitSprite = Helper.settings?.getSpriteName(Categories.CHARACTERS, Portraits.MRP_MALE)
            } else {
                person.portraitSprite = Helper.settings?.getSpriteName(Categories.CHARACTERS, Portraits.MRP_FEMALE)
            }
            Global.getSector().memoryWithoutUpdate[MADMAN_KEY] = person
        }
        return person!!
    }

    override fun updateInteractionDataImpl() {
        val self = if (person.gender == FullName.Gender.MALE) ExternalStrings.MRP_HIMSELF else ExternalStrings.MRP_HERSELF
        val mad = if (person.gender == FullName.Gender.MALE) ExternalStrings.MRP_MADMAN else ExternalStrings.MRP_MADWOMAN
        val inhosp = Helper.sector?.playerFaction
            ?.getRelationshipLevel(RoiderFactions.ROIDER_UNION)
            ?.isAtBest(RepLevel.INHOSPITABLE) == true
        set(HIM_HER, person.himOrHer)
        set(HIM_HERSELF, self)
        set(MADMAN_WOMAN, mad)
        set(MemoryKeys.MRP_REQ_SP, sPRequired)
        set(IS_INHOSP, inhosp)
    }

    override fun callAction(
        action: String?,
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token>?,
        memoryMap: Map<String, MemoryAPI?>?
    ): Boolean {
        if (Helper.anyNull(dialog, memoryMap)) return false

        if (action == ACTION_SHOW) {
            showPicker(dialog!!, memoryMap ?: mapOf())
            return true
        }
        if (action == ACTION_SP) {
            set(MemoryKeys.MRP_REQ_SP, false)
            sPRequired = false
            return true
        }
        if (action == ACTION_CAN_PICK) {
            if (availableShips.isEmpty()) {
                dialog?.optionPanel?.setEnabled(OPTION_SHOW_PICKER, false)
                dialog?.optionPanel?.setTooltip(OPTION_SHOW_PICKER, ExternalStrings.MRP_NO_MIDAS_SHIPS)
            }
            return true
        }
        return true
    }

    private fun showPicker(dialog: InteractionDialogAPI, memoryMap: Map<String, MemoryAPI?>) {
        val avail: List<FleetMemberAPI> = availableShips
        val rows = avail.size / Helper.DIALOG_COLUMNS + 1
        dialog.showFleetMemberPickerDialog(
            ExternalStrings.MRP_PICK_SHIP,
            Misc.ucFirst(ExternalStrings.MRP_OK),
            Misc.ucFirst(ExternalStrings.MRP_CANCEL),
            rows,
            Roider_APRAccess.COLUMNS,
            Helper.DIALOG_ICON_SIZE,
            true,
            false,
            avail,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    if (members.isEmpty()) return
                    val variant: ShipVariantAPI = members[0].variant
                    variant.removePermaMod(RoiderHullmods.MIDAS)
                    variant.hullMods += RoiderHullmods.MIDAS
                    variant.permaMods += RoiderHullmods.MIDAS
                    variant.sModdedBuiltIns += RoiderHullmods.MIDAS
                    val badFlag = Memory.isFlag(MemoryKeys.MRP_BAD_RESULT)
                    val addIllAdvised = genRandom.nextBoolean() && !badFlag
                    if (addIllAdvised) {
                        Memory.setFlag(MemoryKeys.MRP_BAD_RESULT, MemoryKeys.MRP_ILL_ADVISED)
                        variant.addPermaMod(HullMods.ILL_ADVISED)
                        DModManager.setDHull(variant)
                        Memory.set(MemoryKeys.MRP_ILL_ADVISED, true, 0f, memoryMap[MemKeys.LOCAL])
                    } else {
                        Memory.unsetFlag(MemoryKeys.MRP_BAD_RESULT, MemoryKeys.MRP_ILL_ADVISED)
                    }
                    variant.source = VariantSource.REFIT
                    FireBest.fire(null, dialog, memoryMap, TRIGGER_POST_TEXT)
                    Memory.set(MemoryKeys.MRP_PICKED, true, 0f, memoryMap[MemKeys.LOCAL])
                    FireBest.fire(null, dialog, memoryMap, TRIGGER_PICKED)
                }

                override fun cancelledFleetMemberPicking() {
                    Memory.set(MemoryKeys.MRP_PICKED, false, 0f, memoryMap[MemKeys.LOCAL])
                    FireBest.fire(null, dialog, memoryMap, TRIGGER_PICKED)
                }
            })
    }

    private val availableShips: List<FleetMemberAPI>
        get() {
            val pool: CampaignFleetAPI =
                FleetFactoryV3.createEmptyFleet(Factions.PLAYER, FleetTypes.MERC_PRIVATEER, null)
            for (m in Helper.sector?.playerFleet?.fleetData?.membersListCopy ?: listOf()) {
                if (Helper.hasBuiltInMIDAS(m)) continue
                pool.fleetData.addFleetMember(m)
            }
            return pool.fleetData.membersListCopy
        }

    override fun accept(dialog: InteractionDialogAPI, memoryMap: Map<String, MemoryAPI>) {
        currentStage = Any()
        abort()
    }
}