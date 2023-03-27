package net.corda.samples.contractsdk.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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
class IssueRecordPlayerFlow     /*
     * A new record player is issued only from the manufacturer to an exclusive dealer.
     * Most of the settings are default
     */(private val dealer: Party, private val needle: String) : FlowLogic<SignedTransaction?>() {
    // We will not use these ProgressTracker for this Hello-World sample
    override val progressTracker = ProgressTracker()
    private var manufacturer: Party? = null

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {

        // ideally this is only run by the manufacturer
        manufacturer = ourIdentity
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2
        var n = Needle.SPHERICAL
        if (needle == "elliptical") {
            n = Needle.ELLIPTICAL
        }
        val output = RecordPlayerState(manufacturer!!, dealer, n, 100, 700, 10000, 0, UniqueIdentifier())

        // Create a new TransactionBuilder object.
        val builder = TransactionBuilder(notary)

        // Add the iou as an output state, as well as a command to the transaction builder.
        builder.addOutputState(output)
        builder.addCommand(RecordPlayerContract.Commands.Issue(), Arrays.asList(manufacturer!!.owningKey, dealer.owningKey))

        // verify and sign it with our KeyPair.
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Collect the other party's signature using the SignTransactionFlow.
        val otherParties: MutableList<Party> = output.participants.stream().map { el: AbstractParty? -> el as Party? }.collect(Collectors.toList())
        otherParties.remove(ourIdentity)

//        FlowSession targetSession = initiateFlow(this.dealer);
//        return subFlow(new FinalityFlow(ptx, Collections.singletonList(targetSession)));
        val sessions = otherParties.stream().map { el: Party? -> initiateFlow(el!!) }.collect(Collectors.toList())
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        // Assuming no exceptions, we can now finalise the transaction
        return subFlow(FinalityFlow(stx, sessions))
    }
}
