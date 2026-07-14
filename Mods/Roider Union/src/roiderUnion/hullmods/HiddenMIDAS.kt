package roiderUnion.hullmods
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.terrain.MagicAsteroidBeltTerrainPlugin
import roiderUnion.ids.hullmods.RoiderHullmods
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.helpers.StringToken
import roiderUnion.ids.ShipsAndWings
import roiderUnion.cleanup.MadMIDASHealer
import java.awt.Color
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Author: SafariJohn
 */
class HiddenMIDAS : MIDAS() {
    override fun isApplicableToShip(ship: ShipAPI?): Boolean = false
}