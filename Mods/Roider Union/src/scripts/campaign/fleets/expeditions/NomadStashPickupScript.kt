package scripts.campaign.fleets.expeditions

import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.thoughtworks.xstream.XStream
import roiderUnion.nomads.minefields.MajorLootStashPlugin
import java.util.*

/**
 * Author: SafariJohn
 */
class NomadStashPickupScript(fleet: CampaignFleetAPI, target: SectorEntityToken?) : Script {
    companion object {
        const val STASH_ENTITY_KEY = "roider_majorStash"
        const val NULL_ENTITY_KEY = "roider_nullStash"
        fun aliasAttributes(x: XStream) {
            x.aliasAttribute(NomadStashPickupScript::class.java, "token", "t")
            x.aliasAttribute(NomadStashPickupScript::class.java, "fleet", "f")
        }

        fun genMinorStashAndAdd(fleet: CampaignFleetAPI, random: Random?) {
            var expRandom = random
            if (expRandom == null) expRandom = Random()
            val system: LocationAPI = fleet.containingLocation
            val picker = WeightedRandomPicker<String>()
            picker.add(Entities.WRECK, 50f)
            picker.add(Entities.WEAPONS_CACHE, 4f)
            picker.add(Entities.WEAPONS_CACHE_SMALL, 10f)
            picker.add(Entities.WEAPONS_CACHE_HIGH, 4f)
            picker.add(Entities.WEAPONS_CACHE_SMALL_HIGH, 10f)
            picker.add(Entities.WEAPONS_CACHE_LOW, 4f)
            picker.add(Entities.WEAPONS_CACHE_SMALL_LOW, 10f)
            picker.add(Entities.SUPPLY_CACHE, 4f)
            picker.add(Entities.SUPPLY_CACHE_SMALL, 10f)
            picker.add(Entities.EQUIPMENT_CACHE, 4f)
            picker.add(Entities.EQUIPMENT_CACHE_SMALL, 10f)
            val lootId: String = picker.pick()
            if (lootId == Entities.WRECK) {
                val factions: List<String> = listOf(Factions.INDEPENDENT)
                picker.clear()
                picker.addAll(factions)
                var iter = 0
                do {
                    if (iter > 110) break
                    val faction: String = picker.pick()
                    val params: DerelictShipData? = DerelictShipEntityPlugin.createRandom(
                        faction,
                        null,
                        expRandom,
                        DerelictShipEntityPlugin.getDefaultSModProb()
                    )
                    iter++
                    if (params != null) {
                        if (params.ship.getVariant().hullSize == HullSize.CAPITAL_SHIP && iter < 100) {
                            continue
                        }
                        val wreck: CustomCampaignEntityAPI = BaseThemeGenerator.addSalvageEntity(
                            expRandom, system,
                            Entities.WRECK, Factions.NEUTRAL, params
                        ) as CustomCampaignEntityAPI
                        addWreckToFleet(fleet, wreck)
                        break
                    }
                } while (true)
            } else {
                val e: CustomCampaignEntityAPI = system.addCustomEntity(null, "", lootId, Factions.NEUTRAL, null)
                genDropAndAdd(fleet, e)
            }
            fleet.fleetData.sort()
        }

        fun genMajorStashAndAdd(fleet: CampaignFleetAPI, random: Random?) {
            var rand = random
            if (rand == null) rand = Random()
            val system: LocationAPI = fleet.containingLocation
            val picker = WeightedRandomPicker<String>()
            picker.add(Entities.STATION_RESEARCH, 0.1f)
            picker.add(Entities.STATION_MINING, 1f)
            picker.add(Entities.ORBITAL_HABITAT, 5f)
            picker.add(Entities.WRECK, 50f)
            val lootId: String = picker.pick()
            if (lootId == Entities.WRECK) {
                val factions: List<String> = listOf(Factions.INDEPENDENT)
                picker.clear()
                picker.addAll(factions)
                var iter = 0
                do {
                    if (iter > 110) break
                    val faction: String = picker.pick()
                    val params: DerelictShipData? = DerelictShipEntityPlugin.createRandom(
                        faction,
                        null,
                        rand,
                        DerelictShipEntityPlugin.getDefaultSModProb()
                    )
                    iter++
                    if (params != null) {
                        if (params.ship.getVariant().hullSize != HullSize.CAPITAL_SHIP && iter < 100) {
                            continue
                        }
                        val wreck: CustomCampaignEntityAPI = BaseThemeGenerator.addSalvageEntity(
                            rand, system,
                            Entities.WRECK, Factions.NEUTRAL, params
                        ) as CustomCampaignEntityAPI
                        addWreckToFleet(fleet, wreck)
                        break
                    }
                } while (true)
            } else {
                val e: CustomCampaignEntityAPI = system.addCustomEntity(null, "", lootId, Factions.NEUTRAL, null)
                genDropAndAdd(fleet, e)
            }
            fleet.fleetData.sort()
        }

        fun genDropAndAdd(fleet: CampaignFleetAPI, e: SectorEntityToken) {
            if (!e.hasTag(Tags.HAS_INTERACTION_DIALOG)) return
            BaseThemeGenerator.genCargoFromDrop(e)
            val dropV: List<SalvageEntityGenDataSpec.DropData> = e.dropValue
            val dropR: List<SalvageEntityGenDataSpec.DropData> = e.dropRandom
            for (data in dropV) {
                fleet.addDropValue(data.clone())
            }
            for (data in dropR) {
                fleet.addDropRandom(data.clone())
            }
        }

        fun pickWreck(fleet: CampaignFleetAPI, e: SectorEntityToken, major: Boolean) {
            val factions: List<String> = listOf(Factions.INDEPENDENT)
            val random = Random(e.memoryWithoutUpdate.getLong(MemFlags.SALVAGE_SEED))
            val picker = WeightedRandomPicker<String>(random)
            picker.addAll(factions)
            var iter = 0
            do {
                if (iter > 110) break
                val faction: String = picker.pick()
                val params = DerelictShipEntityPlugin.createRandom(
                    faction,
                    null,
                    random,
                    DerelictShipEntityPlugin.getDefaultSModProb()
                )
                iter++
                if (params != null) {
                    val size: HullSize = params.ship.getVariant().hullSize
                    if (major && size != HullSize.CAPITAL_SHIP && iter < 100) {
                        continue
                    }
                    if (!major && size == HullSize.CAPITAL_SHIP && iter < 100) {
                        continue
                    }
                    val salEntity: CustomCampaignEntityAPI = BaseThemeGenerator.addSalvageEntity(
                        random, e.containingLocation,
                        Entities.WRECK, Factions.NEUTRAL, params
                    ) as CustomCampaignEntityAPI
                    e.containingLocation.removeEntity(salEntity)
                    addWreckToFleet(fleet, salEntity)
                    break
                }
            } while (true)
        }

        fun addWreckToFleet(fleet: CampaignFleetAPI, e: SectorEntityToken) {
            val plugin: CustomCampaignEntityPlugin? = e.customPlugin
            if (plugin != null && plugin is DerelictShipEntityPlugin) {
                val data: DerelictShipData = plugin.data
                val mem: FleetMemberAPI = fleet.fleetData.addFleetMember(data.ship.variantId)
                mem.setVariant(data.ship.getVariant(), false, false)
                mem.repairTracker.isMothballed = true
                mem.variant.addTag(Tags.SHIP_RECOVERABLE)
            }
            fleet.fleetData.sort()
        }
    }

    private var token: SectorEntityToken?
    private val fleet: CampaignFleetAPI

    init {
        token = target
        this.fleet = fleet
        if (target == null) {
            token = fleet.containingLocation.createToken(0f, 0f)
            token!!.addTag(NULL_ENTITY_KEY)
        }
    }

    override fun run() {
        if (token!!.hasTag(NULL_ENTITY_KEY)) {
            genMinorStashAndAdd(fleet, null)
            Misc.fadeAndExpire(token)
            token = null
            return
        }
        if (token!!.isExpired) return
        if (!token!!.hasTag(Tags.HAS_INTERACTION_DIALOG) && token!!.customData.containsKey(STASH_ENTITY_KEY)) {
            val stash = token!!.customData[STASH_ENTITY_KEY]
            if (stash != null && stash is SectorEntityToken && !stash.isExpired) {
                if (stash.customPlugin is MajorLootStashPlugin) {
                    val random = Random(stash.memoryWithoutUpdate.getLong(MemFlags.SALVAGE_SEED))
                    val picker = WeightedRandomPicker<String>(random)
                    picker.add(Entities.STATION_RESEARCH, 0.1f)
                    picker.add(Entities.STATION_MINING, 1f)
                    picker.add(Entities.ORBITAL_HABITAT, 5f)
                    picker.add(Entities.WRECK, 50f)
                    val type: String = picker.pick()
                    if (type == Entities.WRECK) {
                        pickWreck(fleet, stash, true)
                    } else {
                        genDropAndAdd(fleet, stash)
                    }
                } else {
                    if (stash.customEntityType == Entities.WRECK) {
                        addWreckToFleet(fleet, stash)
                    } else {
                        genDropAndAdd(fleet, stash)
                    }
                }
                Misc.fadeAndExpire(stash)
            }
        } else if (token!!.customEntityType != null && token!!.customEntityType == Entities.WRECK) {
            addWreckToFleet(fleet, token!!)
        } else {
            genDropAndAdd(fleet, token!!)
        }
        Misc.fadeAndExpire(token)
        token = null
    }
}