package net.corda.samples.contractsdk.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.contractsdk.contracts.RecordPlayerContract
import net.corda.samples.contractsdk.states.Needle
import net.corda.samples.contractsdk.states.RecordPlayerState
import java.util.*
import java.util.stream.Collectors

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
class UpdateRecordPlayerFlow(private val stateId: UniqueIdentifier, private val needleId: String, private val magneticStrength: Int, private val coilTurns: Int, private val amplifierSNR: Int, private val songsPlayed: Int) : FlowLogic<SignedTransaction?>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val listOfLinearIds = Arrays.asList(stateId.id)
        val queryCriteria: QueryCriteria = LinearStateQueryCriteria(null, listOfLinearIds)
        val (states) = serviceHub.vaultService.queryBy(RecordPlayerState::class.java, queryCriteria)
        val inputStateAndRef = states[0] as StateAndRef<*>
        val input = (states[0] as StateAndRef<*>).state.data as RecordPlayerState
        val manufacturer = input.manufacturer
        val dealer = input.dealer
        val needle = when (needleId) {
            "spherical" -> Needle.SPHERICAL
            "elliptical" -> Needle.ELLIPTICAL
            "damaged" -> Needle.DAMAGED
            else -> {
                throw IllegalArgumentException("Invalid needle state given.")
            }
        }
        if (ourIdentity === input.dealer) {
            throw IllegalArgumentException("Only the dealer that sold this record player can service it!")
        }
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2
        val command = Command(
                RecordPlayerContract.Commands.Update(),
                Arrays.asList(manufacturer.owningKey, dealer.owningKey)
        )

        // Create a new TransactionBuilder object.
        val builder = TransactionBuilder(notary)

        // add an output state
        builder.addInputState(inputStateAndRef)
        builder.addOutputState(input.update(needle, magneticStrength, coilTurns, amplifierSNR, songsPlayed), RecordPlayerContract.ID)
        builder.addCommand(command)

        // Verify and sign it with our KeyPair.
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> = input.participants.stream().map { el: AbstractParty? -> el as Party? }.collect(Collectors.toList())
        otherParties.remove(ourIdentity)
        val sessions = otherParties.stream().map { el: Party? -> initiateFlow(el!!) }.collect(Collectors.toList())
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        // Assuming no exceptions, we can now finalise the transaction
        return subFlow(FinalityFlow(stx, sessions))
    }

}
