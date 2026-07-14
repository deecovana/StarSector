package roiderUnion.retrofits.blueprints

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin
import com.fs.starfarer.api.campaign.impl.items.ShipBlueprintItemPlugin
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retroLib.RetrofitsKeeper
import retroLib.impl.BaseRetrofitAdjuster
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.Aliases
import roiderUnion.retrofits.RoiderAllFilter
import java.awt.Color
import java.util.*
import kotlin.collections.ArrayList

/**
 * Author: SafariJohn
 */
class RetrofitBlueprintPlugin : BaseSpecialItemPlugin() {
    companion object {
        fun alias(x: XStream) {
            val jClass = RetrofitBlueprintPlugin::class.java
            x.alias(Aliases.RETBP, jClass)
            x.aliasAttribute(jClass, "ship", "s")
            x.aliasAttribute(jClass, "sources", "o")
        }

        const val TOKEN_SHIP_NAME_CLASS = "\$shipNameClass"
        const val TOKEN_SHIP_DESC = "\$shipDesc"
        const val TOKEN_TAB = "\$tab"
        const val TOKEN_NOUN = "\$noun"
    }

    private var ship: ShipHullSpecAPI? = null
    private var sources: List<String> = ArrayList()
    override fun init(stack: CargoStackAPI) {
        super.init(stack)
        ship = Helper.settings?.getHullSpec(stack.specialDataIfSpecial.data)
        sources = getSources()
    }

    override fun render(
        x: Float, y: Float, w: Float, h: Float, alphaMult: Float,
        glowMult: Float, renderer: SpecialItemPlugin.SpecialItemRendererAPI
    ) {
        val cx = x + w / 2f
        val cy = y + h / 2f
        val blX = cx - 22f
        val blY = cy - 13f
        val tlX = cx - 13f
        val tlY = cy + 25f
        val trX = cx + 26f
        val trY = cy + 25f
        val brX = cx + 19f
        val brY = cy - 14f
        val hullId: String = stack.specialDataIfSpecial.data
        val known = Helper.sector?.playerFaction?.knowsShip(hullId) == true
        val mult = 1f
        val bgColor = Misc.setAlpha(Helper.sector?.playerFaction?.darkUIColor, 255)
        renderer.renderBGWithCorners(
            bgColor, blX, blY, tlX, tlY, trX, trY, brX, brY,
            alphaMult * mult, glowMult * 0.5f * mult, false
        )
        renderer.renderShipWithCorners(
            hullId, null, blX, blY, tlX, tlY, trX, trY, brX, brY,
            alphaMult * mult, glowMult * 0.5f * mult, !known
        )
        val overlay = Helper.settings?.getSprite("ui", "bpOverlayShip") ?: return
        overlay.color = Color.green
        overlay.color = Helper.sector?.playerFaction?.brightUIColor
        overlay.alphaMult = alphaMult
        overlay.setNormalBlend()
        renderer.renderScanlinesWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, false)
        if (known) {
            renderer.renderBGWithCorners(
                Color.black, blX, blY, tlX, tlY, trX, trY, brX, brY,
                alphaMult * 0.5f, 0f, false
            )
        }
        overlay.renderWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY)
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        if (ship != null) {
            val base: Float = super.getPrice(market, submarket).toFloat()
            return (base + ship!!.baseValue * itemPriceMult / 2f).toInt()
        }
        return super.getPrice(market, submarket) / 2
    }

    val providedShip: String?
        get() = ship?.hullId

    override fun getName(): String {
        return if (ship != null) {
            ExternalStrings.RETROFIT_BP_NAME.replace(TOKEN_SHIP_NAME_CLASS, ship!!.nameWithDesignationWithDashClass)
        } else super.getName()
    }

    override fun getDesignType(): String? {
        return if (ship != null) {
            ship!!.manufacturer
        } else null
    }

    override fun createTooltip(
        tooltip: TooltipMakerAPI, expanded: Boolean,
        transferHandler: CargoTransferHandlerAPI, stackSource: Any
    ) {
        val opad = 10f
        tooltip.addTitle(name)
        val design = designType
        Misc.addDesignTypePara(tooltip, design, 10f)
        if (spec.desc.isNotEmpty()) {
            tooltip.addPara(spec.desc, Misc.getGrayColor(), opad)
        }
        val hullId: String = stack.specialDataIfSpecial.data
        val playerFaction = Helper.sector?.playerFaction ?: return
        val known: Boolean = playerFaction.knowsShip(hullId)
        var sourceKnown = sources.isEmpty()
        for (source in sources) {
            if (playerFaction.knowsShip(source)) {
                sourceKnown = true
                break
            }
        }
        val hulls: MutableList<String> = ArrayList()
        hulls.add(hullId)
        addShipList(tooltip, ExternalStrings.RETROFIT_BP_SHIPS, hulls, 1, opad)
        val desc = Helper.settings?.getDescription(ship?.descriptionId, Description.Type.SHIP) ?: return
        val prefix = if (ship?.descriptionPrefix != null) {
            ExternalStrings.RETROFIT_BP_PREFIX.replace(TOKEN_SHIP_DESC, ship!!.descriptionPrefix)
        } else {
            ""
        }
        tooltip.addPara(prefix + desc.text1FirstPara, opad)
        if (sources.isNotEmpty()) addSourceShips(tooltip, ExternalStrings.RETROFIT_BP_SOURCES, sources, 10, opad)
        addCostLabel(tooltip, opad, transferHandler, stackSource)
        if (known) {
            tooltip.addPara(ExternalStrings.RETROFIT_BP_KNOWN, Misc.getGrayColor(), opad)
        } else if (sourceKnown) {
            tooltip.addPara(ExternalStrings.RETROFIT_BP_LEARN, Misc.getPositiveHighlightColor(), opad)
        } else {
            tooltip.addPara(ExternalStrings.RETROFIT_BP_KNOW_ONE, Misc.getNegativeHighlightColor(), opad)
        }
    }

    override fun hasRightClickAction(): Boolean {
        return true
    }

    override fun shouldRemoveOnRightClickAction(): Boolean {
        val hullId = stack.specialDataIfSpecial?.data ?: ExternalStrings.DEBUG_NULL
        val playerFaction = Helper.sector?.playerFaction ?: return true
        if (playerFaction.knowsShip(hullId)) return false
        for (source in sources) {
            if (playerFaction.knowsShip(source)) return true
        }
        return sources.isEmpty()
    }

    override fun performRightClickAction() {
        val hullId = stack.specialDataIfSpecial?.data ?: ExternalStrings.DEBUG_NULL
        val playerFaction = Helper.sector?.playerFaction ?: return
        val known = playerFaction.knowsShip(hullId)
        var sourceKnown = false
        for (source in sources) {
            if (playerFaction.knowsShip(source)) {
                sourceKnown = true
                break
            }
        }
        val display = Helper.sector?.campaignUI?.messageDisplay ?: return
        if (known) {
            display.addMessage(
                ExternalStrings.RETROFIT_BP_ALREADY_KNOWN.replace(TOKEN_SHIP_NAME_CLASS, ship!!.nameWithDesignationWithDashClass)
            )
        } else if (sourceKnown) {
            Helper.soundPlayer?.playUISound("ui_acquired_blueprint", 1f, 1f)
            playerFaction.addKnownShip(hullId, true)
            display.addMessage(
                ExternalStrings.RETROFIT_BP_ACQUIRED.replace(TOKEN_SHIP_NAME_CLASS, ship!!.nameWithDesignationWithDashClass)
            )
        } else {
            display.addMessage(
                ExternalStrings.RETROFIT_BP_KNOW_ONE_EXT.replace(TOKEN_SHIP_NAME_CLASS, ship!!.nameWithDesignationWithDashClass)
            )
        }
    }

    @Throws(JSONException::class)
    override fun resolveDropParamsToSpecificItemData(params: String?, random: Random): String? {
        if (params.isNullOrEmpty()) return null
        val json = JSONObject(params)
        val tags: MutableSet<String> = HashSet()
        if (json.has("tags")) {
            val tagsArray: JSONArray = json.getJSONArray("tags")
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }
        return ShipBlueprintItemPlugin.pickShip(tags, random)
    }

    private fun getSources(): List<String> {
        val src: MutableSet<String> = HashSet()
        val allData = RetrofitsKeeper.getRetrofits(RoiderAllFilter(BaseRetrofitAdjuster(null)))
        allData.filter { it.target == ship?.hullId }.forEach { src += it.source }
        return ArrayList(src)
    }

    fun addSourceShips(tooltip: TooltipMakerAPI, title: String?, ids: List<String>?, max: Int, opad: Float) {
        val idsList = ids ?: emptyList()
        val lister: BlueprintLister = object : BlueprintLister {
            override fun isKnown(id: String): Boolean {
                return Helper.sector?.playerFaction?.knowsShip(id) == true
            }

            override fun getNoun(num: Int): String {
                return if (num == 1) ExternalStrings.RETROFIT_BP_HULL else ExternalStrings.RETROFIT_BP_HULLS
            }

            override fun getName(id: String): String {
                val spec = try {
                    Helper.settings?.getHullSpec(id)
                } catch (_: Exception) {
                    null
                }
                return spec?.nameWithDesignationWithDashClass ?: ExternalStrings.DEBUG_NULL
            }
        }
        tooltip.addPara(title, opad)
        val tab = "        "
        val small = 5f
        var pad = small
        var left = idsList.size
        Collections.sort(idsList, Collections.reverseOrder())
        val idsCopy = mutableListOf<String>()
        for (id in idsList) {
            if (!lister.isKnown(id)) idsCopy.add(id)
        }
        for (id in idsList) {
            if (lister.isKnown(id)) idsCopy.add(id)
        }
        for (id in idsCopy) {
            val known: Boolean = lister.isKnown(id)
            if (known) {
                tooltip.addPara(
                    ExternalStrings.RETROFIT_BP_KNOWN_SHIP.replace(TOKEN_TAB, tab).replace(TOKEN_SHIP_NAME_CLASS, lister.getName(id)),
                    Misc.getButtonTextColor(),
                    pad
                )
            } else {
                tooltip.addPara(
                    ExternalStrings.RETROFIT_BP_UNKNOWN.replace(TOKEN_TAB, tab).replace(TOKEN_SHIP_NAME_CLASS, lister.getName(id)),
                    Misc.getGrayColor(),
                    pad
                )
            }
            left--
            pad = 3f
            if (idsCopy.size - left >= max - 1) break
        }
        if (idsCopy.isEmpty()) {
            tooltip.addPara(ExternalStrings.RETROFIT_BP_NONE.replace(TOKEN_TAB, tab), pad)
        }
        if (left > 0) {
            val noun: String = lister.getNoun(left)
            tooltip.addPara(
                ExternalStrings.RETROFIT_BP_AND_OTHER.replace(TOKEN_TAB, tab).replace(TOKEN_NOUN, noun),
                pad,
                Misc.getHighlightColor(),
                left.toString()
            )
        }
    }
}