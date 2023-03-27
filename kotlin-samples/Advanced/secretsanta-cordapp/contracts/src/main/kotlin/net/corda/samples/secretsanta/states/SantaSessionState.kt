package net.corda.samples.secretsanta.states

import com.sun.istack.NotNull
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.ConstructorForDeserialization
import net.corda.samples.secretsanta.contracts.SantaSessionContract
import java.util.*


// *********
// * State *
// *********
@BelongsToContract(SantaSessionContract::class)
class SantaSessionState : ContractState, LinearState {
    // private variables
    override var linearId: UniqueIdentifier
    var playerNames: List<String?>
    var playerEmails: List<String>
    private var assignments: LinkedHashMap<String?, String?>? = null
    var issuer // issuer is the creator of the santa session
            : Party
    var owner // owner is the receiver of the santa session
            : Party

    // Note Corda always needs a constructor for deserialization in java!
    @ConstructorForDeserialization
    constructor(linearId: UniqueIdentifier, playerNames: List<String?>, playerEmails: List<String>, assignments: LinkedHashMap<String?, String?>?, issuer: Party, owner: Party) {
        this.linearId = linearId
        this.playerNames = playerNames
        this.playerEmails = playerEmails
        this.assignments = assignments
        this.issuer = issuer
        this.owner = owner
    }

    constructor(playerNames: List<String?>, playerEmails: List<String>, issuer: Party, owner: Party) {
        this.playerNames = playerNames
        this.playerEmails = playerEmails
        this.issuer = issuer
        this.owner = owner
        linearId = UniqueIdentifier()
        if (playerNames.size != playerEmails.size) {
            throw IllegalArgumentException("Inconsistent number of names and emails")
        }
        if (playerNames.size < 3) {
            throw IllegalArgumentException("Too few players: " + playerNames.size + " " + playerNames.toString())
        }
        val _assignments = LinkedHashMap<String?, String?>()
        val shuffledPersons = ArrayList(playerNames)
        Collections.shuffle(shuffledPersons)
        for (i in 0 until playerNames.size - 1) {
            _assignments[shuffledPersons[i]] = shuffledPersons[i + 1]
        }
        _assignments[shuffledPersons[shuffledPersons.size - 1]] = shuffledPersons[0]
        setAssignments(_assignments)
    }

    // confirm quality of assignments
    fun setAssignments(assignments: LinkedHashMap<String?, String?>?) {
        if (assignments == null) {
            throw IllegalArgumentException("Given null assignments")
        }
        if (playerNames.size != assignments.size) {
            throw IllegalArgumentException("Invalid number of pairings")
        }
        // iterate through assignments for validity
        for (santa_candidate in playerNames) { // ensure all these players exist
            if (!playerNames.contains(santa_candidate)) {
                throw IllegalArgumentException("A santa candidate does not exist in the list of players")
            }
            for (target_candidate in playerNames) { // skip duplicates in iteration
                if (santa_candidate == target_candidate) {
                    continue
                }
                // ensure no one is assigned themselves
                if (santa_candidate == assignments[santa_candidate]) {
                    throw IllegalArgumentException("player assigned themselves")
                }
            }
        }
        // if assignments look good, we'll set them in the member variable
        this.assignments = assignments
    }

    fun getAssignments(): LinkedHashMap<String?, String?>? {
        return assignments
    }

    /* *
     * This method will indicate who are the participants and required signers when
     * this state is used in a transaction.
     * */
    @get:NotNull
    override val participants: List<AbstractParty>
        get() = Arrays.asList(issuer, owner)

    // convenience method to get a random element from a set
    fun <E> chooseRandomElement(s: Set<String?>): Any? {
        val item = Random().nextInt(s.size)
        var i = 0
        for (obj in s) {
            if (i == item) return obj
            i++
        }
        return null
    }

    /* *
     * get key by value from HashMap
     * from StackOverflow: https://stackoverflow.com/questions/8112975/get-key-from-a-hashmap-using-the-value
     */
    fun getKeyByFirstValue(map: LinkedHashMap<String, String>, v: Any): Any? {
        for ((key, value) in map) {
            if (v == value) {
                return key
            }
        }
        return null
    }
}
