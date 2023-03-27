package net.corda.samples.obligation.server

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.NodeInfo
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.workflows.getCashBalances
import net.corda.samples.obligation.flows.IOUIssueFlow
import net.corda.samples.obligation.flows.IOUTransferFlow
import net.corda.samples.obligation.flows.IOUSettleFlow
import net.corda.samples.obligation.flows.SelfIssueCashFlow
import net.corda.samples.obligation.states.IOUState
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/api/iou/") // The paths for requests are relative to this base path.
class MainController(rpc: NodeRPCConnection) {

    private val proxy = rpc.proxy
    private val me = proxy.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    fun X500Name.toDisplayString() : String  = BCStyle.INSTANCE.toString(this)

    /** Helpers for filtering the network map cache. */
    private fun isNotary(nodeInfo: NodeInfo) = proxy.notaryIdentities().any { nodeInfo.isLegalIdentity(it) }
    private fun isMe(nodeInfo: NodeInfo) = nodeInfo.legalIdentities.first().name == me
    private fun isNetworkMap(nodeInfo : NodeInfo) = nodeInfo.legalIdentities.single().name.organisation == "Network Map Service"


    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoami() = mapOf("me" to me.toString())

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, List<String>> {
        return mapOf("peers" to proxy.networkMapSnapshot()
                .filter { isNotary(it).not() && isMe(it).not() && isNetworkMap(it).not() }
                .map { it.legalIdentities.first().name.toX500Name().toDisplayString() })
    }

    /**
     * Task 1
     * Displays all IOU states that exist in the node's vault.
     * TODO: Return a list of IOUStates on ledger
     * Hint - Use [rpcOps] to query the vault all unconsumed [IOUState]s
     */
    @GetMapping(value = [ "ious" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getIOUs(): List<StateAndRef<ContractState>> {
        // Filter by state type: IOU.
        return proxy.vaultQueryBy<IOUState>().states
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GetMapping(value = [ "cash" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getCash(): List<StateAndRef<ContractState>> {
        // Filter by state type: Cash.
        return proxy.vaultQueryBy<Cash.State>().states
    }

    /**
     * Displays all cash states that exist in the node's vault.
     */
    @GetMapping(value = [ "cash-balances" ], produces = [ APPLICATION_JSON_VALUE ])
    // Display cash balances.
    fun getCashBalances() = proxy.getCashBalances()

    /**
     * Initiates a flow to agree an IOU between two parties.
     * Example request:
     * curl -X PUT 'http://localhost:10007/api/iou/issue-iou?amount=99&currency=GBP&party=O=ParticipantC,L=New%20York,C=US'
     */
    @PutMapping(value = [ "issue-iou" ], produces = [ TEXT_PLAIN_VALUE ])
    fun issueIOU(@RequestParam(value = "amount") amount: Int,
                 @RequestParam(value = "currency") currency: String,
                 @RequestParam(value = "party") party: String): ResponseEntity<String> {
        // Get party objects for myself and the counterparty.
        val me = proxy.nodeInfo().legalIdentities.first()
        val lender = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?: throw IllegalArgumentException("Unknown party name.")
        // Create a new IOU state using the parameters given.
        try {
            val state = IOUState(Amount(amount.toLong() * 100, Currency.getInstance(currency)), lender, me)
            // Start the IOUIssueFlow. We block and waits for the flow to return.
            val result = proxy.startTrackedFlow(::IOUIssueFlow, state).returnValue.get()
            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single()}")

            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (e: Exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.message)

        }
    }

    /**
     * Transfers an IOU specified by [linearId] to a new party.
     * Example request:
     * curl -X GET 'http://localhost:10007/api/iou/transfer-iou?id=705dc5c5-44da-4006-a55b-e29f78955089&party=O=ParticipantC,L=New%20York,C=US'
     */
    @GetMapping(value = [ "transfer-iou" ], produces = [ TEXT_PLAIN_VALUE ])
    fun transferIOU(@RequestParam(value = "id") id: String,
                    @RequestParam(value = "party") party: String): ResponseEntity<String> {
        val linearId = UniqueIdentifier.fromString(id)
        val newLender = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(party)) ?: throw IllegalArgumentException("Unknown party name.")
        return try {
            proxy.startFlow(::IOUTransferFlow, linearId, newLender).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("IOU $id transferred to $party.")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    /**
     * Settles an IOU. Requires cash in the right currency to be able to settle.
     * Example request:
     * curl -X GET 'http://localhost:10007/api/iou/settle-iou?id=705dc5c5-44da-4006-a55b-e29f78955089&amount=98&currency=USD'
     */
    @GetMapping(value = [ "settle-iou" ], produces = [ TEXT_PLAIN_VALUE ])
    fun settleIOU(@RequestParam(value = "id") id: String,
                  @RequestParam(value = "amount") amount: Int,
                  @RequestParam(value = "currency") currency: String): ResponseEntity<String> {
        val linearId = UniqueIdentifier.fromString(id)
        val settleAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))

        return try {
            proxy.startFlow(::IOUSettleFlow, linearId, settleAmount).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("$amount $currency paid off on IOU id $id.")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    /**
     * Helper end-point to issue some cash to ourselves.
     * Example request:
     * curl -X GET 'http://localhost:10007/api/iou/self-issue-cash?amount=100&currency=USD'
     */
    @GetMapping(value = [ "self-issue-cash" ], produces = [ TEXT_PLAIN_VALUE ])
    fun selfIssueCash(@RequestParam(value = "amount") amount: Int,
                      @RequestParam(value = "currency") currency: String): ResponseEntity<String> {
        val issueAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))

        return try {
            val cashState = proxy.startFlow(::SelfIssueCashFlow, issueAmount).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body(cashState.toString())

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

}
