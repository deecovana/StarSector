package roiderUnion.ids

import com.fs.starfarer.api.impl.campaign.ids.Conditions

object RoiderIds {
    const val PREFIX = "roider"
    const val DESC = "_desc" // Appended to an entity's ID get its descriptions.csv ID

    val PERMANENT_STAFF = "permanent_staff"

    object Abilities {
        const val ARGOS_RETROFITS = "roider_retrofit"
    }

    object Roider_Conditions {
        // From Unknown Skies
        const val PARASITE_SPORES = "US_mind"
        const val PSYCHOACTIVE_FUNGUS = "US_shrooms"

        val ORE_CONDITIONS = mapOf(
            Pair(Conditions.ORE_ULTRARICH, 1.0),
            Pair(Conditions.ORE_RICH, 5.0),
            Pair(Conditions.ORE_ABUNDANT, 10.0),
            Pair(Conditions.ORE_MODERATE, 10.0),
            Pair(Conditions.ORE_SPARSE, 10.0),
        )
        val RARE_ORE_CONDITIONS = mapOf(
            Pair(Conditions.RARE_ORE_ULTRARICH, 1.0),
            Pair(Conditions.RARE_ORE_RICH, 5.0),
            Pair(Conditions.RARE_ORE_ABUNDANT, 10.0),
            Pair(Conditions.RARE_ORE_MODERATE, 10.0),
            Pair(Conditions.RARE_ORE_SPARSE, 10.0),
        )
        val ORGANICS_CONDITIONS = mapOf(
            Pair(Conditions.ORGANICS_PLENTIFUL, 5.0),
            Pair(Conditions.ORGANICS_ABUNDANT, 10.0),
            Pair(Conditions.ORGANICS_COMMON, 10.0),
            Pair(Conditions.ORGANICS_TRACE, 5.0),
        )
        val VOLATILES_CONDITIONS = mapOf(
            Pair(Conditions.VOLATILES_PLENTIFUL, 5.0),
            Pair(Conditions.VOLATILES_ABUNDANT, 10.0),
            Pair(Conditions.VOLATILES_DIFFUSE, 10.0),
            Pair(Conditions.VOLATILES_TRACE, 5.0),
        )
    }

    object Entities {
        const val LOOT_STASH = "roider_loot_stash"
        const val LOOT_STASH_MAJOR = "roider_loot_stash_major"
        const val ROCKPIPER_PERCH = "roider_station_rockpiper"
        const val NOMAD_MARKET_ID = "roider_nomadBase_"
        const val SIDE_STATION_0 = "station_side00"
        const val SIDE_STATION_5 = "station_side05"
        const val SIDE_STATION_6 = "station_side06"
        const val MINING_STATION = "station_mining00"
        const val MIDLINE_2 = "station_midline2"
        const val NOMAD_OUTPOST = "roider_nomad_outpost"
        const val NOMAD_JUNK = "roider_nomad_junk"
    }

    object DropGroups {
        const val MARKET_BPS = "roider_market_blueprints"
    }

    object Pings {
        const val PING_TRAP = "roider_ping_trap"
    }

    object Roider_Ranks {
        const val POST_BASE_COMMANDER = "roider_baseCommander"
    }

    object Sounds {
        const val SENSOR_BURST_ON_UI = "ui_sensor_burst_on"
        const val SENSOR_BURST_ON_WORLD = "world_sensor_burst_on"
    }

    object Music {
        const val ROIDER_SYSTEM_BG = "roider_star_system"
        const val RETROFIT_MUSIC = "roider_retrofit"
    }

    object Roider_Submarkets {
        const val UNION_MARKET = "roider_unionMarket"
        const val NOMAD_MARKET = "roider_nomadMarket"
    }

    object Projectiles {
        const val TRACKER_HAMMER = "roider_hammer_armature"
    }

    object Weapons {
        const val DOLDRUMS = "roider_doldrums"
        const val HAZARD_MRM = "roider_hazard_mrm"
        const val HAMMER_SINGLE = "hammer_single"
        const val STINGER_BOMB = "fragbomb"
    }

    object ShipSystems {
        const val TEMPORAL_SHELL = "temporalshell"
        const val DAMPER_FIELD = "damper"
        const val STATION_DRONES = "roider_station_drones"
    }

    object Skills {
        const val APTITUDE_ROAMING = "roider_roaming"
        const val NOMADIC = "roider_roaming_nomadic"
        const val SALVAGING = "roider_roaming_salvaging"
        const val RESOLVE = "roider_roaming_resolve"
        const val HAULING = "roider_roaming_hauling"
        const val INTERCEPTION = "roider_roaming_interception"
        const val CREW_PROTECTION = "roider_roaming_crewProtection"
        const val RINGER_MODS = "roider_roaming_ringerMods"
        const val HULKS = "roider_roaming_hulks"
        const val ROCKPIPER_ENGINEERS = "roider_roaming_rockpiperEngineers"
        const val STORM = "roider_roaming_storm"
    }

}