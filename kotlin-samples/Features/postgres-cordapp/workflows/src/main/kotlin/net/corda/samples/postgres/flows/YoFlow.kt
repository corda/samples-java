package net.corda.samples.postgres.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.postgres.contracts.YoContract
import net.corda.samples.postgres.states.YoState
import java.util.*


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class YoFlow(private val target: Party) : FlowLogic<SignedTransaction?>() {

    override val progressTracker: ProgressTracker = tracker()

    companion object {
        object CREATING : ProgressTracker.Step("Creating a new Yo!")
        object SIGNING : ProgressTracker.Step("Verifying the Yo!")
        object VERIFYING : ProgressTracker.Step("Verifying the Yo!")
        object FINALISING : ProgressTracker.Step("Sending the Yo!") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }


    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING
        val me = ourIdentity

        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2
        val command = Command(YoContract.Commands.Send(), Arrays.asList(me.owningKey))
        val state = YoState(me, target)
        val stateAndContract = StateAndContract(state, YoContract.ID)
        val utx = TransactionBuilder(notary).withItems(stateAndContract, command)
        progressTracker.currentStep = VERIFYING
        utx.verify(serviceHub)
        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(utx)

        progressTracker.currentStep = FINALISING
        val targetSession = initiateFlow(target)

        return subFlow(FinalityFlow(stx, listOf(targetSession), FINALISING.childProgressTracker()))
    }

}

@InitiatedBy(YoFlow::class)
class YoFlowResponder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
