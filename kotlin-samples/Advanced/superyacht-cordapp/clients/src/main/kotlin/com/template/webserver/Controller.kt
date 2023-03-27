package net.corda.samples.example.webserver

import com.template.states.YachtState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import net.corda.client.jackson.JacksonSupport
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.flows.CreateAndIssueYachtStateFlow.CreateAndIssueYachtStateFlowInitiator
import com.template.flows.IssueFiatCurrencyFlow
import com.template.flows.PurchaseYachtFlow.PurchaseYachtFlowInitiator
import net.corda.core.contracts.Amount
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.Currency
import javax.servlet.http.HttpServletRequest


val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {


    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    @Bean
    open fun mappingJackson2HttpMessageConverter(@Autowired rpcConnection: NodeRPCConnection): MappingJackson2HttpMessageConverter {
        val mapper = JacksonSupport.createDefaultMapper(rpcConnection.proxy)
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }


    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    /**
     * Returns the node's name.
     */
    @GetMapping(value = ["me"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = ["peers"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
            .map { it.legalIdentities.first().name }
            //filter out myself, notary and eventual network map started by driver
            .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all Yacht states that exist in the node's vault.
     */
    @GetMapping(value = ["yachts"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getYachts() : ResponseEntity<List<StateAndRef<YachtState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<YachtState>().states)
    }

    /**
     * Displays all the Token states that exist in the node's vault.
     */
    @GetMapping(value = ["tokens"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTokens() : ResponseEntity<List<StateAndRef<FungibleToken>>>{
        return ResponseEntity.ok(proxy.vaultQueryBy<FungibleToken>().states)
    }

    /**
     * Displays all Yacht states that only this node has been involved in.
     */
    @GetMapping(value = ["my-yachts"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMyYachtss(): ResponseEntity<List<StateAndRef<YachtState>>> {
        val myYachts = proxy.vaultQueryBy<YachtState>().states.filter { it.state.data.owner == (proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(myYachts)
    }


    /**
     * Displays all the Token states that this node is a holder of.
     */
    @GetMapping(value = ["my-tokens"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMyTokens() : ResponseEntity<List<StateAndRef<FungibleToken>>>{
        val myTokens = proxy.vaultQueryBy<FungibleToken>().states.filter { it.state.data.holder == (proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(myTokens)
    }

    /**
     * Initiates a flow to create a Fiat Currency Token by the bank and issue it to a Party.
     * Example Request: curl -X PUT "http://localhost:50005/issue-token?amount=100000&currency=USD&recipient=O=PartyB,L=New%20York,C=US"
     */
    @PutMapping(value = ["issue-token"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun issueFiatCurrency(@RequestParam(value = "amount") amount: Int,
                          @RequestParam(value = "currency") currency: String,
                          @RequestParam(value = "recipient") recipient: String): ResponseEntity<String> {


        val me = proxy.nodeInfo().legalIdentities.first()
        val tokenRecipient = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(recipient)) ?: throw IllegalArgumentException("Unknown recipient name.")
//             Issue a new fiat currency token to recipient using the parameters given.
        return try {

            // Start the IOUIssueFlow. We block and waits for the flow to return.
            val result = proxy.startTrackedFlow(::IssueFiatCurrencyFlow, currency, amount.toLong(), tokenRecipient).returnValue.get()
            // Return the response.
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Transaction id ${result.id} committed to ledger.\"Issued $amount $currency token(s) to $recipient \"")

            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.message)
        }
    }

    @PutMapping(value=["create-yacht"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun createAndIssueYachtState(request: HttpServletRequest): ResponseEntity<String> {
        val owner = request.getParameter("owner")
        val name = request.getParameter("name")
        val type = request.getParameter("type")
        val length = request.getParameter("length").toDouble()
        val builderName = request.getParameter("builderName")
        val yearOfBuild = request.getParameter("yearOfBuild").toInt()
        val amount = request.getParameter("amount").toLong()
        val currency = request.getParameter("currency")
        val forSale = request.getParameter("forSale").toBoolean()

        val yachtOwner = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(owner)) ?: throw IllegalArgumentException("Unknown owner name.")

        // Create a new Yacht state using the parameters given.
        return try {
            val result = proxy.startTrackedFlowDynamic(CreateAndIssueYachtStateFlowInitiator::class.java, yachtOwner, name, type, length, builderName, yearOfBuild, amount, currency, forSale).returnValue.getOrThrow()
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Transaction id ${result.id} committed to ledger.\"The yacht's owner is $owner\"")

            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.message)
        }
    }

    /**
     * Initiates a flow to allow a Party (yacht buyer) to purchase a yacht in exchange for a token paid to another Party (yacht owner).
     * Example Request: curl -X PUT "http://localhost:50005/issue-token?amount=100000&currency=USD&recipient=O=PartyB,L=New%20York,C=US"
     */
    @PutMapping (value = ["purchase-yacht"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun purchaseYacht(request: HttpServletRequest): ResponseEntity<String> {
        val newOwner = request.getParameter("newOwner")
        val newOwnerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(newOwner)) ?: throw IllegalArgumentException("Unknown new owner name.")
        val yachtLinearId = request.getParameter("yachtLinearId") ?: return ResponseEntity.badRequest().body("Unknown yacht linear ID.")

        return try {
            val result = proxy.startTrackedFlow(::PurchaseYachtFlowInitiator, newOwnerParty, yachtLinearId).returnValue.getOrThrow()
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Transaction id ${result.id} committed to ledger.\"The yacht's new owner is $newOwner\"")

            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.message)
        }
    }
}

