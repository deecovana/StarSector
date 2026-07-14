package retroLib.impl

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.Misc
import retroLib.ExternalStrings
import retroLib.RetrofitDelivery
import retroLib.api.RetrofitPluginModel
import java.awt.Color

open class BaseRetrofitPluginModel : RetrofitPluginModel {
    override lateinit var dialog: InteractionDialogAPI
    override var optionId: String? = null
    override var targets: List<FleetMemberAPI> = listOf()
    override var currTarget: FleetMemberAPI? = null
    override var selectedShips: List<FleetMemberAPI> = listOf()
    override var queued: List<RetrofitDelivery> = listOf()
    override var queueState: QueueState = QueueState.EMPTY
    override var pickSourcesState: PickSourcesOptionState = PickSourcesOptionState.NORMAL
    override var availableSources: List<FleetMemberAPI> = listOf()
    override var needCommission: Boolean = false
    override var isLowRep: Boolean = false
    override var factionName: String = ExternalStrings.DEBUG_NULL
    override var firstSourceName: String = ExternalStrings.DEBUG_NULL
    override var firstTargetName: String = ExternalStrings.DEBUG_NULL
    override var firstDaysRemaining: Int = 0
    override var queuedDays: Int = 0
    override var queuedMembers: List<FleetMemberAPI> = listOf()
    override var factionColor: Color = Misc.getTextColor()
    override var targetsAvailable: List<FleetMemberAPI> = listOf()
    override var targetsUnavailable: List<FleetMemberAPI> = listOf()
    override var targetsIllegal: List<FleetMemberAPI> = listOf()
    override var targetsNotAllowed: List<FleetMemberAPI> = listOf()
    override var sourceTextData: List<SourceTextData> = listOf()
    override var confirmationCost: Float = 0f
    override var sModRefundXP: Float = 0f
}