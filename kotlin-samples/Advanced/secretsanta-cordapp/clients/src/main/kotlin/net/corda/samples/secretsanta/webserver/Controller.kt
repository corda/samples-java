package net.corda.samples.secretsanta.webserver

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.identity.Party
import net.corda.core.internal.toX500Name
import net.corda.core.node.NodeInfo
import net.corda.samples.secretsanta.flows.CheckAssignedSantaFlow
import net.corda.samples.secretsanta.flows.CreateSantaSessionFlow
import net.corda.samples.secretsanta.states.SantaSessionState
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
@CrossOrigin(origins = ["*"])
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

    private fun getPartyFromNodeInfo(nodeInfo: NodeInfo): Party {
        return nodeInfo.legalIdentities[0]
    }

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


    @RequestMapping(value = ["/node"], method = [RequestMethod.GET])
    private fun returnName(): ResponseEntity<String> {
        val resp = JsonObject()
        resp.addProperty("name", me.toString())
        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(resp.toString())
    }


    @RequestMapping(value = ["/games/check"], method = [RequestMethod.POST])
    fun checkGame(@RequestBody payload: String?): ResponseEntity<String?>? {
        println(payload)
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val gameId = fromString(convertedObject["gameId"].asString)
        // NOTE lowercase the name for easy retrieve
        val playerName = "\"" + convertedObject["name"].asString.toLowerCase().trim { it <= ' ' } + "\""

        // optional param
        val sendEmail = convertedObject["sendEmail"].asBoolean
        val resp = JsonObject()
        return try {
            val output = proxy.startTrackedFlowDynamic(CheckAssignedSantaFlow::class.java, gameId).returnValue.get()
            if (output.getAssignments()!![playerName] == null) {
                resp.addProperty("target", "target not found in this game")
                return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(resp.toString())
            }
            val playerNames = output.playerNames
            val playerEmails = output.playerEmails
            val playerEmail = playerEmails[playerNames.indexOf(playerName)]
            val targetName = output.getAssignments()!![playerName]!!.replace("\"", "")
            resp.addProperty("target", targetName)
            if (sendEmail) {
                println("sending email to $playerEmail")
                val b: Boolean = sendEmail(playerEmail, craftNotice(playerName, targetName, gameId))
                if (!b) {
                    println("ERROR: Failed to send email.")
                }
            }
            ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @RequestMapping(value = ["/games"], method = [RequestMethod.POST])
    fun createGame(@RequestBody payload: String?): ResponseEntity<String?>? {
        println(payload)
        val convertedObject = Gson().fromJson(payload, JsonObject::class.java)
        val pNames = convertedObject.getAsJsonArray("playerNames")
        val pMails = convertedObject.getAsJsonArray("playerEmails")

        // optional param
        val sendEmail = convertedObject["sendEmail"].asBoolean
        val playerNames: MutableList<String> = ArrayList()
        val playerEmails: MutableList<String> = ArrayList()

        // NOTE: we lowercase all names internally for clarity
        for (jo in pNames) {
            val newName = jo.toString().toLowerCase()
            if (!playerNames.contains(newName)) {
                playerNames.add(newName)
            }
        }
        for (jo in pMails) {
            playerEmails.add(jo.toString())
        }
        return try {
            val elf: Party = getPartyFromNodeInfo(proxy.networkMapSnapshot()[2])

            // run the flow to create our game
            val output = proxy.startTrackedFlowDynamic(CreateSantaSessionFlow::class.java, playerNames, playerEmails, elf).returnValue.get().tx.outputsOfType(SantaSessionState::class.java)[0]
            val gameId = output.linearId
            val assignments = output.getAssignments()
            println("Created Secret Santa Game ID# " + output.linearId.toString())

            // send email to each player with the assignments
            for (p in playerNames) {
                val t = assignments!![p]!!.replace("\"", "")
                val msg: String = craftNotice(p, t, gameId)
                val ind = playerNames.indexOf(p)
                val email = playerEmails[ind].replace("\"", "")
                if (sendEmail) {
                    val b: Boolean = sendEmail(email, msg)
                    if (!b) {
                        println("ERROR: Failed to send email.")
                    }
                }
            }
            val resp = JsonObject()
            resp.addProperty("gameId", gameId.toString())
            ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(resp.toString())
        } catch (e: java.lang.Exception) {
            println("Exception : " + e.message)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }


    fun craftNotice(p: String, t: String, gameId: UniqueIdentifier): String {
        return "Hello, $p!\nYour super secret santa target for game # $gameId is $t."
    }

    fun sendEmail(recipient: String?, msg: String?): Boolean {

        // TODO replace the below line with your *REAL* api key!
        val SENDGRID_API_KEY = "SG.xxxxxx_xxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx_xxxxxxxxxxxxxxxxxx_xxxxxxx"
        if (SENDGRID_API_KEY == "SG.xxxxxx_xxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx_xxxxxxxxxxxxxxxxxx_xxxxxxx") {
            println("You haven't added your sendgrid api key!")
            return true
        }

        // you'll need to specify your sendgrid verified sender identity for this to mail out.
        val from = Email("test@example.com")
        val subject = "Secret Assignment from the Elves"
        val to = Email(recipient)
        val content = Content("text/plain", msg)
        val mail = Mail(from, subject, to, content)
        val sg = SendGrid(SENDGRID_API_KEY)
        val request = Request()
        return try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response = sg.api(request)
            println(response.statusCode)
            println(response.body)
            println(response.headers)
            true
        } catch (ex: IOException) {
            println(ex.toString())
            false
        }
    }


}
