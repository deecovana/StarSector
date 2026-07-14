package retroLib.api

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import retroLib.RetrofitDelivery
import retroLib.impl.PickSourcesOptionState
import retroLib.impl.QueueState
import retroLib.impl.SourceTextData
import java.awt.Color

interface RetrofitPluginModel {
    var dialog: InteractionDialogAPI
    var optionId: String?
    var targets: List<FleetMemberAPI>
    var currTarget: FleetMemberAPI?
    var selectedShips: List<FleetMemberAPI>
    var queued: List<RetrofitDelivery>
    var queueState: QueueState
    var availableSources: List<FleetMemberAPI>
    var pickSourcesState: PickSourcesOptionState
    var needCommission: Boolean
    var isLowRep: Boolean
    var factionName: String
    var firstSourceName: String
    var firstTargetName: String
    var firstDaysRemaining: Int
    var queuedDays: Int
    var queuedMembers: List<FleetMemberAPI>
    var factionColor: Color
    var targetsAvailable: List<FleetMemberAPI>
    var targetsUnavailable: List<FleetMemberAPI>
    var targetsIllegal: List<FleetMemberAPI>
    var targetsNotAllowed: List<FleetMemberAPI>
    var sourceTextData: List<SourceTextData>
    var confirmationCost: Float
    var sModRefundXP: Float
}