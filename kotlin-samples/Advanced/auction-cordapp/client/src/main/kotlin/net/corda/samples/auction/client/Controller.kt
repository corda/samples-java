package net.corda.samples.auction.client

import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.messaging.CordaRPCOps
import net.corda.samples.auction.flows.*
import net.corda.samples.auction.states.AuctionState
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.corda.finance.workflows.getCashBalance
import net.corda.samples.auction.states.Asset
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api/auction") // The paths for HTTP requests are relative to this base path.
class Controller() {

    @Autowired lateinit var partyAProxy: CordaRPCOps

    @Autowired lateinit var partyBProxy: CordaRPCOps

    @Autowired lateinit var partyCProxy: CordaRPCOps

    @Autowired
    @Qualifier("partyAProxy")
    lateinit var proxy: CordaRPCOps

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    @GetMapping(value = [ "list" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getAuctionList() : APIResponse<List<StateAndRef<AuctionState>>> {
        return APIResponse.success(proxy.vaultQuery(AuctionState::class.java).states)
    }

    @GetMapping(value = [ "asset/list" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getAssetList() : APIResponse<List<StateAndRef<Asset>>> {
        return APIResponse.success(proxy.vaultQuery(Asset::class.java).states)
    }

    @PostMapping(value = ["asset/create"])
    fun createAsset(@RequestBody assetForm: Forms.AssetForm): APIResponse<String> {
        return try {
            proxy.startFlowDynamic(
                    CreateAssetFlow::class.java,
                    assetForm.title,
                    assetForm.description,
                    assetForm.imageUrl
            ).returnValue.get()

            APIResponse.success("Account ${assetForm.title} Created")
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @PostMapping(value = ["create"])
    fun createAuction(@RequestBody auctionForm: Forms.CreateAuctionForm): APIResponse<String> {
        return try {
            proxy.startFlowDynamic(
                    CreateAuctionFlow::class.java,
                    Amount.parseCurrency("${auctionForm.basePrice} USD"),
                    UUID.fromString(auctionForm.assetId),
                    LocalDateTime.parse(auctionForm.deadline, DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a"))
            ).returnValue.get()
            APIResponse.success("Auction ${auctionForm.assetId} Created")
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @PostMapping(value = ["delete/{auctionId}"])
    fun deleteAuction(@PathVariable auctionId:String): APIResponse<String> {
        return try {
            proxy.startFlowDynamic(
                    AuctionExitFlow::class.java,
                    UUID.fromString(auctionId)
            ).returnValue.get()

            APIResponse.success("Auction ${auctionId} deleted")
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @PostMapping(value = ["placeBid"])
    fun placeBid(@RequestBody bidForm: Forms.BidForm): APIResponse<String> {
        return try {
            proxy.startFlowDynamic(
                    BidFlow::class.java,
                    UUID.fromString(bidForm.auctionId),
                    Amount.parseCurrency("${bidForm.amount} USD")
            ).returnValue.get()
            APIResponse.success("Bid ${bidForm.auctionId} placed")
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @PostMapping(value = ["payAndSettle"])
    fun payAndSettle(@RequestBody settlementForm: Forms.SettlementForm): APIResponse<String> {
        return try {
            proxy.startFlowDynamic(
                    AuctionSettlementFlow::class.java,
                    UUID.fromString(settlementForm.auctionId),
                    Amount.parseCurrency("${settlementForm.amount}")
            ).returnValue.get()
            APIResponse.success("Auction ${settlementForm.auctionId} paid")
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @PostMapping(value = ["issueCash"])
    fun issueCash(@RequestBody issueCashForm: Forms.IssueCashForm): APIResponse<String> {
        return try {
            proxy.startFlowDynamic(
                    CashIssueAndPaymentFlow::class.java,
                    Amount.parseCurrency("${issueCashForm.amount} USD"),
                    OpaqueBytes("PartyA".toByteArray()),
                    proxy.partiesFromName(issueCashForm.party!!, false).iterator().next(),
                    false,
                    proxy.notaryIdentities().firstOrNull()
            ).returnValue.get()
            APIResponse.success("Cash issued. Amount: ${issueCashForm.amount}")
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @GetMapping(value = [ "getCashBalance" ])
    fun getCashBalance(): APIResponse<String> {
        return try {
            var amount = proxy.getCashBalance(Currency.getInstance("USD")).quantity

            if(amount >= 100L)
                amount /= 100L

            APIResponse.success("Balance: $amount")
        } catch (e: Exception) {
            handleError(e)
        }
    }

    @PostMapping(value = ["switch-party/{party}"])
    fun switchParty(@PathVariable party:String): APIResponse<String> {
        when (party) {
            "PartyA"-> proxy = partyAProxy
            "PartyB"-> proxy = partyBProxy
            "PartyC"-> proxy = partyCProxy
            else -> return APIResponse.error("Unrecognised Party")
        }
        return getCashBalance()
    }

    @PostMapping(value = ["setup"])
    fun setupDemoData(): APIResponse<String> {

        val dataSetA = SampleDataFactory.getProxyASampleAsset()
        callCreateAssetFlow(partyAProxy, dataSetA)

        val dataSetB = SampleDataFactory.getProxyBSampleAsset()
        callCreateAssetFlow(partyBProxy, dataSetB)

        val dataSetC = SampleDataFactory.getProxyCSampleAsset()
        callCreateAssetFlow(partyCProxy, dataSetC)

        return APIResponse.success("Setup success")
    }

    private fun callCreateAssetFlow(proxy:CordaRPCOps, dataSet: List<SampleAsset>){
        dataSet.forEach {
            proxy.startFlowDynamic(CreateAssetFlow::class.java, it.title, it.description, it.imageUrl)
        }
    }

    private fun handleError(e: Exception): APIResponse<String> {
        logger.error("RequestError", e)
        return when (e) {
            is TransactionVerificationException.ContractRejection ->
                APIResponse.error(e.cause?.message ?: e.message!!)
            else ->
                APIResponse.error(e.message!!)
        }
    }
}