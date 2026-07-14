package roiderUnion.roidMining

import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import roiderUnion.ids.RoiderFactions

class RoidType(
    val id: String,
    val name: String,
    val faction: String,
    val playerFaction: String,
    val baseValue: Float,
    vararg val commodityWeights: Map<String, Float>
) {
    companion object {
        /**
         * Removes group from commodity weights if selected in group
         */
        const val NULL_COM_PICK = ""

        val ICE = RoidType(
            "ice",
            "Ice", // extern
            RoiderFactions.MINING_ICE,
            RoiderFactions.MINING_ICE_PLAYER,
            2800f,
            mapOf(
                Pair(Commodities.VOLATILES, 1f)
            ),
            mapOf(
                Pair(Commodities.ORGANICS, 9f)
            )
        )
        val DUST = RoidType(
            "dust",
            "Dust",
            RoiderFactions.MINING_DUST,
            RoiderFactions.MINING_DUST_PLAYER,
            650f,
            mapOf(
                Pair(Commodities.ORE, 10f),
                Pair(Commodities.ORGANICS, 10f)
            ),
            mapOf(
                Pair(Commodities.ORE, 8f),
                Pair(Commodities.ORGANICS, 8f),
                Pair(NULL_COM_PICK, 16f)
            )
        )
        val ROCK = RoidType(
            "rock",
            "Rock",
            RoiderFactions.MINING_ROCK,
            RoiderFactions.MINING_ROCK_PLAYER,
            1800f,
            mapOf(
                Pair(Commodities.ORE, 10f),
                Pair(Commodities.ORGANICS, 10f)
            ),
            mapOf(
                Pair(Commodities.ORE, 8f),
                Pair(Commodities.ORGANICS, 8f),
                Pair(NULL_COM_PICK, 16f)
            ),
            mapOf(
                Pair(Commodities.RARE_ORE, 10f)
            )
        )
        val METAL = RoidType(
            "metal",
            "Metal",
            RoiderFactions.MINING_METAL,
            RoiderFactions.MINING_METAL_PLAYER,
            3600f,
            mapOf(
                Pair(Commodities.ORE, 10f),
                Pair(Commodities.ORGANICS, 10f)
            ),
            mapOf(
                Pair(Commodities.ORE, 8f),
                Pair(Commodities.ORGANICS, 8f),
                Pair(NULL_COM_PICK, 16f)
            ),
            mapOf(
                Pair(Commodities.RARE_ORE, 36f)
            ),
            mapOf(
                Pair(Commodities.VOLATILES, 18f)
            )
        )
        val PLUTON = RoidType(
            "pluton",
            "Pluton",
            RoiderFactions.MINING_PLUTON,
            RoiderFactions.MINING_PLUTON_PLAYER,
            7200f,
            mapOf(
                Pair(Commodities.VOLATILES, 10f)
            ),
            mapOf(
                Pair(Commodities.RARE_ORE, 10f)
            )
        )
        val NULL = RoidType("null", "None", Factions.NEUTRAL, Factions.NEUTRAL, 0f, emptyMap())
    }

    override fun equals(other: Any?): Boolean {
        if (other is RoidType) {
            return id == other.id
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}