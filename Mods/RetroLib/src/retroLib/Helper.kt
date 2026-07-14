package retroLib

import com.fs.starfarer.api.FactoryAPI
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.SettingsAPI
import com.fs.starfarer.api.SoundPlayerAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.AllianceManager
import exerelin.campaign.PlayerFactionStore
import org.magiclib.kotlin.getSizeNum
import kotlin.math.roundToInt

object Helper {
    const val MANAGER_KEY = "\$retroLib_manager"
    const val HULL_POSTFIX = "_Hull"
    const val BACK_TO_BAR = "backToBar"

    const val TOKEN_NUMBER = "[NUMBER]"

    val modId = "RetroLib"

    fun isModEnabled(id: String): Boolean {
        return settings?.modManager?.isModEnabled(id) == true
    }

    val sector: SectorAPI?
        get() = Global.getSector()

    val settings: SettingsAPI?
        get() = Global.getSettings()

    val factory: FactoryAPI?
        get() = Global.getFactory()

    val soundPlayer: SoundPlayerAPI?
        get() = Global.getSoundPlayer()

    private const val TAG_HEAVY_INDUSTRY = "heavyindustry"
    val HEAVY_INDUSTRIES = mutableListOf(
        Industries.HEAVYINDUSTRY,
        Industries.ORBITALWORKS
    )

    fun marketHasHeavyIndustry(market: MarketAPI?): Boolean {
        if (HEAVY_INDUSTRIES.any { market?.hasIndustry(it) == true }) return true
        return market?.industries?.any { it.spec?.hasTag(TAG_HEAVY_INDUSTRY) == true } == true
    }

    fun anyNull(vararg objects: Any?): Boolean {
        for (o in objects) {
            if (o == null) return true
        }

        return false
    }

    fun calculateCost(
        source: String?,
        target: String?, market: MarketAPI?
    ): Double {
        var results = calculateBuyPrice(target, market)
        results -= calculateSellPrice(source, market)
        results *= Settings.CONVERSION_PRICE_MULT.toDouble()
        val roundThousands = results >= RetrofitsLoader.ROUNDING_THRESHOLD
        return if (roundThousands) roundToThousands(results) else roundToHundreds(results)
    }

    private fun calculateBuyPrice(id: String?, market: MarketAPI?): Double {
        if (anyNull(id)) return 0.0
        val tariff = 1f + (market?.tariff?.modifiedValue ?: 0f)
        val baseValue = try {
            settings?.getHullSpec(id)?.baseValue ?: 0f
        } catch (ex: Exception) {
            try {
                settings?.getFighterWingSpec(id)?.baseValue ?: 0f
            } catch (ex: java.lang.Exception) {
                0f
            }
        }
        val buyMult = Settings.BUY_MULT
        return (baseValue * buyMult * tariff).toDouble()
    }

    private fun calculateSellPrice(id: String?, market: MarketAPI?): Double {
        if (id.isNullOrEmpty()) return 0.0
        val tariff = 1f + (market?.tariff?.modifiedValue ?: 0f)
        val sourceValue = try {
            settings?.getHullSpec(id)?.baseValue ?: 0f
        } catch (ex: Exception) {
            try {
                settings?.getFighterWingSpec(id)?.baseValue ?: 0f
            } catch (ex: java.lang.Exception) {
                0f
            }
        }
        val sellMult = Settings.SELL_MULT
        val sellPrice: Float = sourceValue * sellMult
        return (sellPrice - sellPrice * (tariff - 1f)).toDouble()
    }

    fun roundToThousands(value: Double): Double {
        return ((value / 1000).roundToInt() * 1000).toDouble()
    }

    fun roundToHundreds(value: Double): Double {
        return ((value / 100).roundToInt() * 100).toDouble()
    }

    fun matchesHullId(spec: ShipHullSpecAPI?, source: String): Boolean {
        if (spec == null) return false
        return spec.hullId == source || spec.baseHullId == source
    }

    fun getRetrofitManager(): RetrofitsKeeper {
        val key = MANAGER_KEY
        val producer = { RetrofitsKeeper() }
        val memory = sector?.memoryWithoutUpdate ?: return producer.invoke()
        val t = memory[key]
        return if (t is RetrofitsKeeper) {
            t
        } else {
            val result = producer.invoke()
            memory[key] = result
            result
        }
    }

    fun hasCommission(fitterFaction: String?): Boolean {
        if (fitterFaction == null) return false
        if (ModPlugin.hasNexerelin) {
            if (AllianceManager.areFactionsAllied(PlayerFactionStore.getPlayerFactionId(), fitterFaction )) {
                return true
            }
            val commissionFaction = Misc.getCommissionFactionId()
            val allied = commissionFaction != null && AllianceManager.areFactionsAllied(commissionFaction, fitterFaction)
            if (allied) {
                return true
            }
        }
        return fitterFaction == Misc.getCommissionFactionId()
    }

    fun prepDisplayShip(ship: FleetMemberAPI, legal: Boolean) {
        ship.shipName = "Retrofit to" // extern
        if (!legal) {
            ship.repairTracker.isMothballed = true
            ship.repairTracker.cr = 0f
        } else {
            ship.repairTracker.cr = 0.7f
        }
    }

    fun isFrameHull(member: FleetMemberAPI): Boolean {
        val hullspec = try {
            settings?.getHullSpec(member.hullSpec.baseHullId)
        } catch (ex: Exception) {
            null
        }
        return member.hullSpec.hasTag(RetroLib_Tags.FRAME_HULL)
                || hullspec?.hasTag(RetroLib_Tags.FRAME_HULL) == true
    }

    fun isFrameHull(spec: ShipHullSpecAPI?): Boolean {
        val hullspec = try {
            settings?.getHullSpec(spec?.baseHullId)
        } catch (ex: Exception) {
            null
        }
        return spec?.hasTag(RetroLib_Tags.FRAME_HULL) == true
                || hullspec?.hasTag(RetroLib_Tags.FRAME_HULL) == true
    }

    fun getFrameId(tags: Set<String>, size: ShipAPI.HullSize): String? {
        if (tags.none { it.startsWith(RetroLib_Tags.FRAME_HULL) }) return null
        val tag = tags.filterNot { it == RetroLib_Tags.FRAME_HULL }.firstOrNull { it.startsWith(RetroLib_Tags.FRAME_HULL) } ?: return null
        return settings?.allShipHullSpecs?.firstOrNull { it.hullSize == size && it.tags.contains(tag) }?.hullId
    }

    fun getGivenFrameHull(tags: Set<String>): String? {
        tags.forEach {
            val frame = when (it) {
                RetroLib_Tags.GIVE_FRAME_CAPITAL -> getFrameId(tags, ShipAPI.HullSize.CAPITAL_SHIP)
                RetroLib_Tags.GIVE_FRAME_CRUISER -> getFrameId(tags, ShipAPI.HullSize.CRUISER)
                RetroLib_Tags.GIVE_FRAME_DESTROYER -> getFrameId(tags, ShipAPI.HullSize.DESTROYER)
                RetroLib_Tags.GIVE_FRAME_FRIGATE -> getFrameId(tags, ShipAPI.HullSize.FRIGATE)
                else -> null
            }
            if (frame != null) return frame
        }
        return null
    }

    fun isHullSameOrBigger(hull1: FleetMemberAPI, hull2: FleetMemberAPI): Boolean {
        return isHullSameOrBigger(hull1.hullSpec, hull2.hullSpec)
    }

    fun isHullSameOrBigger(hull1: ShipHullSpecAPI, hull2: ShipHullSpecAPI): Boolean {
        return hull1.hullSize.getSizeNum() >= hull2.hullSize.getSizeNum()
    }

    fun isHullSmaller(hull1: ShipHullSpecAPI, hull2: ShipHullSpecAPI): Boolean {
        return hull1.hullSize.getSizeNum() < hull2.hullSize.getSizeNum()
    }

    fun getFrameHullSize(sourceSize: ShipAPI.HullSize, targetsSize: ShipAPI.HullSize): ShipAPI.HullSize? {
        val targetSizeNum = when (targetsSize) {
            ShipAPI.HullSize.FRIGATE -> 1
            ShipAPI.HullSize.DESTROYER -> 2
            ShipAPI.HullSize.CRUISER -> 3
            ShipAPI.HullSize.CAPITAL_SHIP -> 4
            else -> return null
        }
        val sourceSizeNum = when (sourceSize) {
            ShipAPI.HullSize.FRIGATE -> 1
            ShipAPI.HullSize.DESTROYER -> 2
            ShipAPI.HullSize.CRUISER -> 3
            ShipAPI.HullSize.CAPITAL_SHIP -> 4
            else -> return null
        }
        val newSize = when (sourceSizeNum - targetSizeNum) {
            3 -> ShipAPI.HullSize.CRUISER
            2 -> ShipAPI.HullSize.DESTROYER
            1 -> ShipAPI.HullSize.FRIGATE
            else -> return null
        }
        return newSize
    }

    fun createShip(id: String) : FleetMemberAPI? {
        return factory?.createFleetMember(FleetMemberType.SHIP, id + HULL_POSTFIX)
    }

    fun createWing(id: String) : FleetMemberAPI? {
        return factory?.createFleetMember(FleetMemberType.FIGHTER_WING, id)
    }

    fun floatToPercentString(float: Float): String {
        return ExternalStrings.NUMBER_PERCENT.replace(TOKEN_NUMBER, float.roundToInt().toString())
    }

    fun multToPercentString(mult: Float): String {
        return ExternalStrings.NUMBER_PERCENT.replace(TOKEN_NUMBER, (mult * 100f).roundToInt().toString())
    }

    fun getSModReturnBonusXP(size: HullSize, sMods: Int): Float {
        val bonusXPMult = 1f - when (size) {
            HullSize.CAPITAL_SHIP -> settings?.getBonusXP("permModCapital") ?: 1f
            HullSize.CRUISER -> settings?.getBonusXP("permModCruiser") ?: 1f
            HullSize.DESTROYER -> settings?.getBonusXP("permModDestroyer") ?: 1f
            HullSize.FRIGATE -> settings?.getBonusXP("permModFrigate") ?: 1f
            else -> 1f
        }
        return sMods * bonusXPMult.coerceIn(0f, 1f)
    }
}