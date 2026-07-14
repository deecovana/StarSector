package roiderUnion.ids

object MemoryKeys {
    const val TOKEN_NAME = "\$roider_tokenName"

    const val NOMAD_BASE_INDUSTRY = "\$roider_nomadBaseInd"
    const val NOMAD_BASE_LEVEL = "\$roider_nomadBaseLevel"
    const val NOMAD_GROUP = "\$roider_nomadGroup"
    const val NOMAD_GROUPS = "\$roider_nomadGroups"
    const val NOMAD_ACTIVE_GROUPS = "\$roider_nomadActiveGroups"
    const val NOMAD_BASES = "\$roider_nomadBases"
    const val NOMAD_BASE_UNION_CAPITAL = "\$roider_nomadBaseLevel"
    const val NOMAD_BASE_ACTIVE = "\$roider_nomadActive"
    const val NOMAD_BASE_CLAIMED = "\$roider_nomadClaimed"
    const val NOMAD_BASE_INTEL = "\$roider_nomadBaseIntel"
    const val NOMAD_BASE = "\$roider_nomadBase"
    const val NOMAD_BASE_UPGRADING = "\$roider_nomadBaseUpgrading"
    const val NOMAD_OUTPOST = "\$roider_nomadOutpost"
    const val NOMAD_OUTPOST_LEVEL = "\$roider_nomadOutpostLevel"
    const val NOMAD_WARNING = "\$roider_nomadWarning"

    /**
     * Mad Rockpiper
     */
    const val MRP_ILL_ADVISED = "\$roider_mrp_illAdvised"
    const val MRP_PICKED = "\$roider_mrp_picked"
    const val MRP_REQ_SP = "\$roider_mrp_requiresSP"
    const val MRP_DEFAULT_RANT = "\$roider_mrp_defaultRantPick"
    const val MRP_SP_RANT = "\$roider_mrp_spRantPick"
    const val MRP_INHOSP_RANT = "\$roider_mrp_inhospRantPick"
    const val MRP_BAD_RESULT = "\$roider_mrp_badResult"

    // Retrofit managers are kept in their market's memory.
    const val RETROFITTER = "\$roider_retrofitManager"
    const val SW_RETROFITTER = "\$roider_swRetrofitManager"

    const val RETROFIT_KEEPER = "\$roider_retrofitsKeeper"

    // RetrofitAccess variables
    const val TALK_RETROFITS = "\$roider_talkedAboutRetrofits"
    const val FEE_PAID = "\$roider_retrofitFeePaid"
    const val ACCESS_FEE = "\$roider_retrofitFee"
    const val STORAGE_FEE = "\$roider_retrofitStorageFeeAdded"

    const val DEFAULT_TARIFF = "default_tariff"

    // Stored in the market's memory.
    const val UNION_HQ_FUNCTIONAL = "\$roider_hq_functional"
    const val SHIPWORKS_ALPHA = "\$roider_shipworks_alphaCore"
    const val SHIPWORKS_BP_BYPASS = "\$roider_shipworksBypass"

    // Roider Union sub-faction commission
    const val ROIDER_COMMISSION = "\$roider_commission"

    // Blocks dives and Union HQs from mining
    const val CLAIMED = "\$roider_miningBlocked"

    // Mark where fringe Union HQs are
    const val FRINGE_HQ = "\$roider_fringeUnionHQ"

    // Tech expeditions
    const val EXPEDITION_LOOT = "\$roider_expeditionLoot"
    const val EXPEDITION_LOOT_MAJOR = "\$roider_expeditionLootMajor"
    const val EXPEDITION_FACTION = "\$roider_expeditionFaction"
    const val EXPEDITION_MARKET = "\$roider_expeditionSource"
    const val THIEF_KEY = "\$roider_thief"

    const val EXPEDITION_WRECK_PLUGIN = "\$roider_expeditionWreckPlugin"

    // Conversion fleets
    const val APR_OFFERINGS = "\$roider_aprOfferings" // List<String>

    const val APR_RETROFITTING = "\$roider_aprIsRetrofitting"
    const val APR_IGNORE_REP = "\$roider_aprIgnoreRep"
    const val APR_IGNORE_COM = "\$roider_aprIgnoreCommission"
    const val APR_IGNORE_TRANSPONDER = "\$roider_aprIgnoreTransponder"

    // Miner fleets
    const val MINER_SOURCE = "\$roider_minerSource_"
    const val MINER_FACTION_WEIGHTS = "\$roider_minerFactionWeights_"
    const val MINER_RANGE = "\$roider_minerRange"

    const val NO_MINING = "\$roider_noMine"
    const val MINING_SPOT = "\$roider_miningSpot"
    const val RECENTLY_SENT_MINER = "\$roider_recentlySentMiner"

    const val VOID_RESOURCE = "\$roider_voidResource_"

    const val PROSPECTING_BLOCKED = "\$roider_prospectingBlocked"

}