package net.corda.samples.tokentofriend.webserver

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.corda.samples.tokentofriend.flows.CreateMyToken
import net.corda.samples.tokentofriend.flows.IssueToken
import net.corda.samples.tokentofriend.flows.QueryToken
import net.corda.core.internal.toX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.node.NodeInfo
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


/**
 * Define your API endpoints here.
 */
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
    private fun isNetworkMap(nodeInfo: NodeInfo) = nodeInfo.legalIdentities.single().name.organisation == "Network Map Service"

    /**
     * Returns the node's name.
     */
    @GetMapping(value = ["me"], produces = [APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to me.toString())

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GetMapping(value = ["peers"], produces = [APPLICATION_JSON_VALUE])
    fun getPeers(): Map<String, List<String>> {
        return mapOf("peers" to proxy.networkMapSnapshot()
                .filter { isNotary(it).not() && isMe(it).not() && isNetworkMap(it).not() }
                .map { it.legalIdentities.first().name.toX500Name().toDisplayString() })
    }

    @RequestMapping(value = ["/createToken"], method = [RequestMethod.POST])
    @Throws(Exception::class)
    fun createToken(@RequestBody payload: String?): ResponseEntity<String> {
        println(payload)
        val convertedObject: JsonObject = Gson().fromJson(payload, JsonObject::class.java)
        val sender = convertedObject.get("senderEmail").toString()
        val senderStr = sender.substring(1,sender.length-1)
        val receiver = convertedObject.get("recipientEmail").toString()
        val receiverStr = receiver.substring(1,receiver.length-1)
        val message = convertedObject.get("secretMessage").toString()
        val messageStr = message.substring(1,message.length-1)

        println(senderStr)
        println(receiverStr)
        println(messageStr)
        return try {
            val tokenStateId = proxy.startFlow(::CreateMyToken,senderStr,receiverStr,messageStr).returnValue.get().toString()
            val result = proxy.startFlow(::IssueToken,tokenStateId).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body(result)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @RequestMapping(value = ["/retrieve"], method = [RequestMethod.POST])
    @Throws(Exception::class)
    fun retrieveToken(@RequestBody payload: String?): ResponseEntity<String> {
        println(payload)
        val convertedObject: JsonObject = Gson().fromJson(payload, JsonObject::class.java)
        val tokenId = convertedObject.get("tokenId").toString()
        val tokenIdStr = tokenId.substring(1,tokenId.length-1)
        val receiver = convertedObject.get("recipientEmail").toString()
        val receiverStr = receiver.substring(1,receiver.length-1)
        return try {
            val result = proxy.startFlow(::QueryToken,tokenIdStr,receiverStr).returnValue.get().toString()
            print(result)
            ResponseEntity.status(HttpStatus.CREATED).body(result)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }

    }




}