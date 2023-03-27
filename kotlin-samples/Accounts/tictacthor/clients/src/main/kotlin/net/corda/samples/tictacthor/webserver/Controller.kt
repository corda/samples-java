package net.corda.samples.tictacthor.webserver

import net.corda.samples.tictacthor.accountsUtilities.CreateNewAccount
import net.corda.samples.tictacthor.accountsUtilities.ShareAccountTo
import net.corda.samples.tictacthor.flows.EndGameFlow
import net.corda.samples.tictacthor.flows.StartGameFlow
import net.corda.samples.tictacthor.flows.SubmitTurnFlow
import net.corda.samples.tictacthor.accountsUtilities.myGame
import net.corda.samples.tictacthor.states.BoardState
import net.corda.samples.tictacthor.states.Status
import net.corda.core.internal.toX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.NodeInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

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

    @PostMapping(value = ["createAccount/{acctName}"])
    fun createAccount(@PathVariable acctName:String):ResponseEntity<String> {
        return try {
            val result = proxy.startFlow(::CreateNewAccount,acctName).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Account $acctName Created")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @PostMapping(value = ["requestGameWith/{whoAmI}/{team}/{competeWith}"])
    fun requestGameWith(@PathVariable whoAmI:String,@PathVariable team:String, @PathVariable competeWith:String):ResponseEntity<String> {
        val matchingPasties = proxy.partiesFromName(team,false)
        return try {
            val result = proxy.startFlow(::ShareAccountTo,whoAmI,matchingPasties.first()).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("Game Request has Sent. When $competeWith accepts your challenge, the game will start!")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }


    @PostMapping(value = ["acceptGameInvite/{whoAmI}/{team}/{competeWith}"])
    fun acceptGameInvite(@PathVariable whoAmI:String,@PathVariable team:String, @PathVariable competeWith:String):ResponseEntity<String> {
        val matchingPasties = proxy.partiesFromName(team,false)
        return try {
            val result = proxy.startFlow(::ShareAccountTo,whoAmI,matchingPasties.first()).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("I, $whoAmI accepts $competeWith's challenge. Let's play!")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @PostMapping(value = ["startGameAndFirstMove/{whoAmI}/{competeWith}/{position}"])
    fun startGameAndFirstMove(@PathVariable whoAmI:String,
                              @PathVariable competeWith:String,
                              @PathVariable position: String):ResponseEntity<String> {
        var x :Int = -1
        var y :Int = -1
        when(position.toInt()) {
            0 -> {x=0; y=0}
            1 -> {x=1; y=0}
            2 -> {x=2; y=0}
            3 -> {x=0; y=1}
            4 -> {x=1; y=1}
            5 -> {x=2; y=1}
            6 -> {x=0; y=2}
            7 -> {x=1; y=2}
            8 -> {x=2; y=2}
        }
        return try {
            val gameId = proxy.startFlow(::StartGameFlow,whoAmI,competeWith).returnValue.get()!!
            val submitTurn = proxy.startFlow(::SubmitTurnFlow, gameId, whoAmI,competeWith,x,y).returnValue.get()!!
            ResponseEntity.status(HttpStatus.CREATED).body("Game Id Created: $gameId" + "$whoAmI made the first move on position [$x,$y]")

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @PostMapping(value = ["submitMove/{whoAmI}/{competeWith}/{position}"])
    fun submitMove(@PathVariable whoAmI:String,
                   @PathVariable competeWith:String,
                   @PathVariable position: String):ResponseEntity<String> {
        var x :Int = -1
        var y :Int = -1
        when(position.toInt()) {
            0 -> {x=0; y=0}
            1 -> {x=1; y=0}
            2 -> {x=2; y=0}
            3 -> {x=0; y=1}
            4 -> {x=1; y=1}
            5 -> {x=2; y=1}
            6 -> {x=0; y=2}
            7 -> {x=1; y=2}
            8 -> {x=2; y=2}
        }
        return try {
            val gameId = proxy.startFlow(::myGame,whoAmI).returnValue.get().linearId
            val submitTurn = proxy.startFlow(::SubmitTurnFlow, gameId, whoAmI,competeWith,x,y).returnValue.get()!!

            if(isGameOver(whoAmI)){
                proxy.startFlow(::EndGameFlow, gameId, whoAmI,competeWith).returnValue.get()
                ResponseEntity.status(HttpStatus.CREATED).body("$whoAmI made the move on position [$x,$y], and Game Over")
            }else{
                ResponseEntity.status(HttpStatus.CREATED).body("$whoAmI made the move on position [$x,$y]")
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
}


    private fun isGameOver(whoAmI: String):Boolean{
        //If the game is over, the Status should be a null variable
        //So if the status returned is GAME_IN_PROGRESS, it means the game is not over.
        val gameStatus = proxy.startFlow(::myGame,whoAmI).returnValue.get().status
        if(gameStatus == Status.GAME_IN_PROGRESS){
            return false
        }
        return true
    }
}