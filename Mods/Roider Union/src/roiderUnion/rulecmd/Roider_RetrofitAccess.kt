package roiderUnion.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AddBarEvent
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.util.Misc
import retroLib.impl.*
import roiderUnion.helpers.Memory
import roiderUnion.econ.DivesInteractor
import roiderUnion.helpers.ExternalStrings
import roiderUnion.helpers.Helper
import roiderUnion.ids.*
import roiderUnion.retrofits.*

/**
 * Author: SafariJohn
 */
class Roider_RetrofitAccess : BaseCommandPlugin() {
    companion object {
        const val OPTION_ADD = "addBarEvent"
        const val OPTION_DESC = "describe"
        const val OPTION_REFUSE = "refuse"
        const val OPTION_PAY = "payFee"
        const val OPTION_RETROFIT = "retrofit"
        const val OPTION_HQ_FUNC = "roiderHQFunctional"

        const val RETROFIT_FEE = 0f // 20000
        const val STORAGE_FEE = 5000f
        const val REFUSE_ID = "roider_retrofitRefuse"
        const val DESCRIBE_ID = "roider_retrofitDescribe"
        const val PAY_ID = "roider_retrofitPay"
        const val STRAIGHT_ID = "roider_retrofitStraight"
        const val STORAGE_ID = "roider_retrofitStorage"
        const val LEAVE_ID = "backToBar"

        const val TOKEN_NAME = "\$personName"
        const val TOKEN_LAST_NAME = "\$personLastName"
        const val TOKEN_RANK = "\$personRank"
        const val TOKEN_HIS_HER = "\$hisOrHer"
        const val TOKEN_HIS_HER_CAP = "\$HisOrHer"
        const val TOKEN_HE_SHE = "\$heOrShe"
        const val TOKEN_HE_SHE_CAP = "\$HeOrShe"
        const val TOKEN_STORAGE = "\$storageCost"
    }

    private lateinit var interactor: BaseRetrofitPluginInteractor
    private lateinit var view: UnionHQPluginView

    private lateinit var dialog: InteractionDialogAPI
    private lateinit var entity: SectorEntityToken
    private lateinit var market: MarketAPI
    private lateinit var person: PersonAPI
    private lateinit var faction: FactionAPI
    private val hisOrHer: String
        get() = if (person.isFemale) ExternalStrings.RETRO_HER else ExternalStrings.RETRO_HIS
    private val heOrShe: String
        get() = if (person.isFemale) ExternalStrings.RETRO_SHE else ExternalStrings.RETRO_HE

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        this.dialog = dialog ?: return false
        val command: String = params?.get(0)?.getString(memoryMap) ?: return false
        market = dialog.interactionTarget?.market ?: return false
        entity = market.primaryEntity ?: return false
        person = createPerson() ?: return false
        faction = person.faction ?: return false
        when (command) {
            OPTION_ADD -> addBarEvent()
            OPTION_DESC -> describeRetrofitting()
            OPTION_REFUSE -> refuseService()
            OPTION_PAY -> payFee(memoryMap)
            OPTION_RETROFIT -> retrofit(memoryMap)
            OPTION_HQ_FUNC -> return roiderHQFunctional()
        }
        return true
    }

    private fun retrofit(memoryMap: MutableMap<String, MemoryAPI>?) {
        val model = BaseRetrofitPluginModel()
        val filter = UnionHQFilter(market, BaseRetrofitAdjuster(market))
        val manager = BaseRetrofitManager(market, faction, filter)
        val service = Memory.get(
            MemoryKeys.RETROFITTER,
            market,
            { it is UnionHQDeliveryService },
            { createDeliveryService(market) }
        ) as UnionHQDeliveryService
        val originalPlugin = dialog.plugin
        interactor = BaseRetrofitPluginInteractor(
            model,
            faction,
            manager,
            service,
            memoryMap ?: mutableMapOf(),
            originalPlugin,
            this::showRetrofittingMessages
        )
        view = UnionHQPluginView(
            model,
            this::updateInteractor
        )
        val plugin = RetrofitPlugin(interactor, view)
        dialog.plugin = plugin
        plugin.init(dialog)
    }

    private fun createDeliveryService(market: MarketAPI) : UnionHQDeliveryService {
        val result = UnionHQDeliveryService(market)
        Helper.sector?.addScript(result)
        return result
    }

    private fun updateInteractor() {
        interactor.update()
    }

    private fun showRetrofittingMessages(cost: Double, stripped: Boolean) {
        view.showRetrofittingMessages(cost, stripped)
    }

    private fun personWillRetrofit(): Boolean {
        return faction.isAtWorst(Factions.PLAYER, RepLevel.NEUTRAL)
    }

    private fun roiderHQFunctional(): Boolean {
        return market.hasFunctionalIndustry(RoiderIndustries.UNION_HQ)
    }

    private fun addBarEvent() {
        // Calculate access cost
        val storagePaid = isStoragePaid(market)
        market = dialog.interactionTarget.market
        val blurb = replaceTokens(ExternalStrings.RETRO_BLURB)
        val option = getOptionText(storagePaid)
        val optionId = if (!personWillRetrofit()) REFUSE_ID
        else if (!isFeePaid) DESCRIBE_ID
        else if (!storagePaid) STORAGE_ID
        else STRAIGHT_ID
        val data = AddBarEvent.BarEventData(optionId, option, blurb)
        data.optionColor = Helper.roiders?.color
        val events: AddBarEvent.TempBarEvents = AddBarEvent.getTempEvents(market)
        events.events[optionId] = data
    }

    private fun replaceTokens(str: String): String {
        return str
            .replace(TOKEN_RANK, person.rank)
            .replace(TOKEN_HE_SHE, heOrShe)
            .replace(TOKEN_HE_SHE_CAP, Misc.ucFirst(heOrShe))
            .replace(TOKEN_HIS_HER, hisOrHer)
            .replace(TOKEN_HIS_HER_CAP, Misc.ucFirst(hisOrHer))
            .replace(TOKEN_LAST_NAME, person.name.last)
            .replace(TOKEN_NAME, person.nameString)
    }

    private fun getOptionText(storagePaid: Boolean): String {
        var result = replaceTokens(ExternalStrings.RETRO_OPTION)
        if (isFirstTime && !isFeePaid) result = replaceTokens(ExternalStrings.RETRO_OPTION_FIRST)
        if (isFeePaid && !storagePaid) {
            result = replaceTokens(ExternalStrings.RETRO_OPTION_PAY
                .replace(TOKEN_STORAGE, Misc.getDGSCredits(fee)))
        }
        if (!isFirstTime && !personWillRetrofit()) result = replaceTokens(ExternalStrings.RETRO_OPTION_TALK)
        return result
    }

    private fun describeRetrofitting() {
        dialog.interactionTarget.activePerson = person
        dialog.visualPanel.showPersonInfo(person, false, true)
        val text: TextPanelAPI = dialog.textPanel
        if (isFirstTime) {
            person.memoryWithoutUpdate.set(MemoryKeys.TALK_RETROFITS, true)
            text.addPara(replaceTokens(ExternalStrings.RETRO_DESC_FIRST))
        } else {
            text.addPara(replaceTokens(ExternalStrings.RETRO_DESC_BACK))
        }
        if (retroLib.Helper.marketHasHeavyIndustry(market)) {
            text.addPara(replaceTokens(ExternalStrings.RETRO_DESC_FULL))
        } else {
            text.addPara(replaceTokens(ExternalStrings.RETRO_DESC_LIGHT))
        }
        text.addPara(replaceTokens(ExternalStrings.RETRO_DESC_EXTRA))
        val fee = fee
        if (!isFeePaid && !isStoragePaid(market)) {
            text.addPara(replaceTokens(ExternalStrings.RETRO_DESC_PAY)
                .replace(TOKEN_STORAGE, Misc.getDGSCredits(fee)))
            text.highlightInLastPara(Misc.getHighlightColor(), Misc.getDGSCredits(fee))
        } else {
            text.addPara(replaceTokens(ExternalStrings.RETRO_DESC_PREPAID))
        }
        val options: OptionPanelAPI = dialog.optionPanel
        options.clearOptions()
        if (this.fee > 0) options.addOption(
            replaceTokens(ExternalStrings.RETRO_DESC_OPTION_PAY.replace(TOKEN_STORAGE, Misc.getDGSCredits(fee))),
            PAY_ID
        ) else options.addOption(ExternalStrings.RETRO_DESC_OPTION, PAY_ID)
        options.addOption(ExternalStrings.RETRO_DESC_OPTION_LEAVE, LEAVE_ID)
        if ((Helper.sector?.playerFleet?.cargo?.credits?.get() ?: 0f) < fee) {
            options.setEnabled(PAY_ID, false)
            options.setTooltip(PAY_ID, ExternalStrings.RETRO_DESC_OPTION_TT_POOR)
        }
    }

    private fun refuseService() {
        dialog.interactionTarget.activePerson = person
        dialog.visualPanel.showPersonInfo(person, false, true)
        val text: TextPanelAPI = dialog.textPanel
        if (!isFirstTime) {
            text.addPara(replaceTokens(ExternalStrings.RETRO_NO_SERVICE))
            return
        }
        person.memoryWithoutUpdate.set(MemoryKeys.TALK_RETROFITS, true)
        text.addPara(replaceTokens(ExternalStrings.RETRO_NO_SERVICE_FIRST1))
        text.addPara(replaceTokens(ExternalStrings.RETRO_NO_SERVICE_FIRST2))
        text.addPara(replaceTokens(ExternalStrings.RETRO_NO_SERVICE_FIRST3))
        text.addPara(replaceTokens(ExternalStrings.RETRO_NO_SERVICE_FIRST4))
    }

    private fun payFee(memoryMap: MutableMap<String, MemoryAPI>?) {
        val fee = fee
        if (fee > 0) {
            Global.getSector().playerFleet.cargo.credits.subtract(fee)
        }
        Helper.roiders?.memoryWithoutUpdate?.set(MemoryKeys.FEE_PAID, true)
        (Misc.getStorage(market) as StoragePlugin).setPlayerPaidToUnlock(true)
        retrofit(memoryMap)
    }

    private val fee: Float
        get() {
            var fee = 0f
            if (isFeePaid && isStoragePaid(market)) fee = 0f
            else {
                if (!isFeePaid) fee += RETROFIT_FEE
                if (!isStoragePaid(market)) fee += STORAGE_FEE
            }
            return fee
        }

    private fun createPerson(): PersonAPI? {
        return if (market.hasIndustry(RoiderIndustries.UNION_HQ)) getBaseCommander() else null
    }

    private fun getBaseCommander(): PersonAPI {
        return Memory.get(
            "\$${RoiderIds.Roider_Ranks.POST_BASE_COMMANDER}",
            market,
            { it is PersonAPI },
            { DivesInteractor.createBaseCommander(market) }
        ) as PersonAPI
    }

    private val isFirstTime: Boolean
        get() = !person.memoryWithoutUpdate.getBoolean(MemoryKeys.TALK_RETROFITS)
    private val isFeePaid: Boolean
        get() = Helper.roiders?.memoryWithoutUpdate?.getBoolean(MemoryKeys.FEE_PAID) ?: false

    private fun isStoragePaid(market: MarketAPI?): Boolean {
        return Misc.playerHasStorageAccess(market)
    }
}