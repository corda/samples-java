package net.corda.samples.logging.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.logging.contracts.YoContract
import net.corda.samples.logging.states.YoState
import org.apache.logging.log4j.ThreadContext
import org.slf4j.LoggerFactory
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
        // note we're creating a logger first with the shared name from our other example.
        val logger = LoggerFactory.getLogger("net.corda")
        progressTracker.currentStep = CREATING
        val me = ourIdentity

        // here we have our first opportunity to log out the contents of the flow arguments.
        ThreadContext.put("initiator", me.name.toString())
        ThreadContext.put("target", target.name.toString())
        // publish to the log with the additional context
        logger.info("Initializing the transaction.")
        // flush the threadContext
        ThreadContext.removeAll(Arrays.asList("initiator", "target"))

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

        // inject details to the threadcontext to be exported as json
        ThreadContext.put("tx_id", stx.id.toString())
        ThreadContext.put("notary", notary!!.name.toString())
        // publish to the log with the additional context
        logger.info("Finalizing the transaction.")
        // flush the threadContext
        ThreadContext.removeAll(Arrays.asList("tx_id", "notary"))
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
