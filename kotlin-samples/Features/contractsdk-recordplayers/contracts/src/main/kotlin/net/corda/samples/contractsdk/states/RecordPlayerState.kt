package net.corda.samples.contractsdk.states

import com.r3.corda.lib.contracts.contractsdk.verifiers.StateWithRoles
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.ConstructorForDeserialization
import java.util.*

import net.corda.samples.contractsdk.contracts.RecordPlayerContract

// *********
// * State *
// *********
@BelongsToContract(RecordPlayerContract::class)
class RecordPlayerState @ConstructorForDeserialization constructor(manufacturer: Party, dealer: Party, needle: Needle?, magneticStrength: Int, coilTurns: Int, amplifierSNR: Int, songsPlayed: Int, uid: UniqueIdentifier) : ContractState, LinearState, StateWithRoles {
    // we'll assume some basic stats about this record player
    var needle // enum describing the needle type or damage
            : Needle?
    var magneticStrength // assume 100 gauss about the strength of a refrigerator magnet
            : Int
    var coilTurns // typical number of turns in the coils of a record player cartridge
            : Int
    var amplifierSNR // signal to noise ratio on the amplifier, 10,000 is pretty good.
            : Int
    val uid // unique id for this rare record player.
            : UniqueIdentifier
    var songsPlayed // assume a new player has not played any tracks and it's in mint condition.
            : Int
    private val manufacturerNotes: String? = null
    var manufacturer: Party
    var dealer: Party
        private set

    /* Constructor for a CordaGraf with default */
    constructor(manufacturer: Party, dealer: Party, needle: Needle?) : this(manufacturer, dealer, needle, 100, 700, 10000, 0, UniqueIdentifier()) {}

    fun setOwner(dealer: Party) {
        this.dealer = dealer
    }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    override val participants: List<AbstractParty>
        get() = Arrays.asList(manufacturer, dealer)

    fun update(needle: Needle?, magneticStrength: Int, coilTurns: Int, amplifierSNR: Int, songsPlayed: Int): RecordPlayerState {
        // take our params and apply them to the state object
        return RecordPlayerState(manufacturer, dealer, needle, magneticStrength, coilTurns, amplifierSNR, songsPlayed, uid)
    }

    override val linearId: UniqueIdentifier
        get() = uid

    override fun getParty(role: String): Party {
        return when (role.toLowerCase()) {
            "manufacturer" -> manufacturer
            "dealer" -> dealer
            else -> manufacturer
        }
    }

    /* Constructor */
    init {
        if (songsPlayed < 0) {
            throw IllegalArgumentException("Invalid songs played")
        }
        if (magneticStrength < 0) {
            throw IllegalArgumentException("Invalid magnetic strength.")
        }
        this.manufacturer = manufacturer
        this.dealer = dealer
        this.needle = needle
        this.magneticStrength = magneticStrength
        this.coilTurns = coilTurns
        this.amplifierSNR = amplifierSNR
        this.songsPlayed = songsPlayed
        this.uid = uid
    }
}
