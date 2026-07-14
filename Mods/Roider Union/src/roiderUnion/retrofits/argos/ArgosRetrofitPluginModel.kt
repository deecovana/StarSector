package roiderUnion.retrofits.argos

import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import retroLib.impl.BaseRetrofitPluginModel
import java.awt.Color

class ArgosRetrofitPluginModel : BaseRetrofitPluginModel() {
    var isPaidConversion: Boolean = true
    var needRez: Boolean = true
    var needCR: Boolean = true
    var argosSourceTextData: List<ArgosSourceTextData> = emptyList()
    var factionId: String = Factions.NEUTRAL
    var factionDarkUIColor: Color = Misc.getDarkPlayerColor()
    var rezCosts: Map<String, Int> = emptyMap()
    var totalRezCost: Int = 0
    var availableDockShips: List<FleetMemberAPI> = emptyList()
    var activeDockShips: List<FleetMemberAPI> = emptyList()
}