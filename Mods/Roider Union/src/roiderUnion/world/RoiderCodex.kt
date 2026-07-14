package roiderUnion.world

import com.fs.starfarer.api.impl.SharedUnlockData
import com.fs.starfarer.api.impl.codex.CodexDataV2
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin
import roiderUnion.helpers.Helper
import roiderUnion.ids.RoiderIds
import roiderUnion.ids.RoiderIndustries
import roiderUnion.ids.RoiderItems
import roiderUnion.ids.ShipsAndWings
import roiderUnion.ids.hullmods.RoiderHullmods

object RoiderCodex {
    fun initializeCodexEntries() {
        val desperadoCodex = getShip(ShipsAndWings.DESPERADO)
        val garruchaDrawCodex = getHullmod(RoiderHullmods.GARRUCHA_DRAW)
        val garruchaCodex = getFighter(ShipsAndWings.GARRUCHA_WING)
        pairEntries(desperadoCodex, garruchaDrawCodex)
        pairEntries(garruchaCodex, garruchaDrawCodex)

        val dives = getIndustry(RoiderIndustries.DIVES)
        val unionHq = getIndustry(RoiderIndustries.UNION_HQ)
        val charter = getItem(RoiderItems.HISTORICAL_CHARTER)
        pairEntries(charter, dives)
        pairEntries(charter, unionHq)

        val divesBP = getItem(RoiderItems.DIVES_BP)
        pairEntries(dives, divesBP)
        val hqBP = getItem(RoiderItems.UNION_HQ_BP)
        pairEntries(unionHq, hqBP)

        val shipworks = getIndustry(RoiderIndustries.SHIPWORKS)
        val swBp = getItem(RoiderItems.SHIPWORKS_BP)
        pairEntries(shipworks, swBp)

        val midas = getHullmod(RoiderHullmods.MIDAS)
        val midas1 = getHullmod(RoiderHullmods.MIDAS_1)
        val midas2 = getHullmod(RoiderHullmods.MIDAS_2)
        val midas3 = getHullmod(RoiderHullmods.MIDAS_3)
        val midasA = getHullmod(RoiderHullmods.MIDAS_ARMOR)
        val midasF = getHullmod(RoiderHullmods.MIDAS_FIGHTER)
        pairEntries(midas, midas1)
        pairEntries(midas, midas2)
        pairEntries(midas, midas3)
        pairEntries(midas, midasF)
        pairEntries(midas, midasA)

        val hopskip = getFighter(ShipsAndWings.HOPSKIP_2_WING)
        val hazard = getWeapon(RoiderIds.Weapons.HAZARD_MRM)
        pairEntries(hopskip, hazard)

        val tracker = getFighter(ShipsAndWings.TRACKER_SINGLE)
        val tracker2 = getFighter(ShipsAndWings.TRACKER_WING)
        val hammer = getWeapon(RoiderIds.Weapons.HAMMER_SINGLE)
        pairEntries(tracker, tracker2)
        pairEntries(tracker, hammer)
        pairEntries(tracker2, hammer)

        val duster = getFighter(ShipsAndWings.DUSTER_WING)
        val stingerBomb = getWeapon(RoiderIds.Weapons.STINGER_BOMB)
        pairEntries(duster, stingerBomb)

        val rocksaw = getFighter(ShipsAndWings.ROCKSAW_WING)
        val doldrums = getWeapon(RoiderIds.Weapons.DOLDRUMS)
        pairEntries(rocksaw, doldrums)

        val rockroach = getShip(ShipsAndWings.ROCKROACH)
        val rockroachArmor = getShip(ShipsAndWings.ROCKROACH_ARMOR)
        val damper = getShipSystem(RoiderIds.ShipSystems.DAMPER_FIELD)
        val temporal = getShipSystem(RoiderIds.ShipSystems.TEMPORAL_SHELL)
        pairEntries(rockroach, damper)
        pairEntries(rockroachArmor, damper)
        pairEntries(rockroach, temporal)
        pairEntries(rockroachArmor, temporal)

        val damperOnagerArmor = getShip(ShipsAndWings.ONAGER_DAMPER_ARMOR)
        pairEntries(damperOnagerArmor, damper)

        val damperRanchArmor = getShip(ShipsAndWings.RANCH_DAMPER_ARMOR)
        pairEntries(damperRanchArmor, damper)

        val stationDroneSystem = getShipSystem(RoiderIds.ShipSystems.STATION_DRONES)
        val stationDroneHidden = getShip(ShipsAndWings.STATION_DRONE_HIDDEN)
        stationDroneSystem?.removeRelatedEntry(stationDroneHidden)
        val stationDrone = getShip(ShipsAndWings.STATION_DRONE)
        pairEntries(stationDroneSystem, stationDrone)


        checkLinkedUnlocks()
    }

    private fun pairEntries(entry1: CodexEntryPlugin?, entry2: CodexEntryPlugin?) {
        if (Helper.anyNull(entry1, entry2)) return
        entry1!!.addRelatedEntry(entry2!!.id)
        entry2.addRelatedEntry(entry1.id)
    }

    fun checkLinkedUnlocks() {
        val instance = SharedUnlockData.get()
        if (instance.isPlayerAwareOfHullmod(RoiderHullmods.GARRUCHA_DRAW)) {
            instance.reportPlayerAwareOfFighter(ShipsAndWings.GARRUCHA_WING, true)
        }

        if (instance.isPlayerAwareOfIndustry(RoiderIndustries.DIVES)) {
            instance.reportPlayerAwareOfSpecialItem(RoiderItems.DIVES_BP, true)
        }
        if (instance.isPlayerAwareOfSpecialItem(RoiderItems.DIVES_BP)) {
            instance.reportPlayerAwareOfIndustry(RoiderIndustries.DIVES, true)
        }

        if (instance.isPlayerAwareOfIndustry(RoiderIndustries.UNION_HQ)) {
            instance.reportPlayerAwareOfSpecialItem(RoiderItems.UNION_HQ_BP, true)
        }
        if (instance.isPlayerAwareOfSpecialItem(RoiderItems.UNION_HQ_BP)) {
            instance.reportPlayerAwareOfIndustry(RoiderIndustries.UNION_HQ, true)
        }

        if (instance.isPlayerAwareOfIndustry(RoiderIndustries.SHIPWORKS)) {
            instance.reportPlayerAwareOfSpecialItem(RoiderItems.SHIPWORKS_BP, true)
        }
        if (instance.isPlayerAwareOfSpecialItem(RoiderItems.SHIPWORKS_BP)) {
            instance.reportPlayerAwareOfIndustry(RoiderIndustries.SHIPWORKS, true)
        }
    }

    private fun getShip(id: String): CodexEntryPlugin? = CodexDataV2.getEntry(CodexDataV2.getShipEntryId(id))
    private fun getFighter(id: String): CodexEntryPlugin? = CodexDataV2.getEntry(CodexDataV2.getFighterEntryId(id))
    private fun getHullmod(id: String): CodexEntryPlugin? = CodexDataV2.getEntry(CodexDataV2.getHullmodEntryId(id))
    private fun getIndustry(id: String): CodexEntryPlugin? = CodexDataV2.getEntry(CodexDataV2.getIndustryEntryId(id))
    private fun getItem(id: String): CodexEntryPlugin? = CodexDataV2.getEntry(CodexDataV2.getItemEntryId(id))
    private fun getShipSystem(id: String): CodexEntryPlugin? = CodexDataV2.getEntry(CodexDataV2.getShipSystemEntryId(id))
    private fun getWeapon(id: String): CodexEntryPlugin? = CodexDataV2.getEntry(CodexDataV2.getWeaponEntryId(id))
}