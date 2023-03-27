package net.corda.samples.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.bikemarket.states.FrameTokenState
import net.corda.samples.bikemarket.states.WheelsTokenState

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TransferBikeToken(val frameSerial: String,
                   val wheelsSerial: String,
                   val holder: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        //Step 1: Frame Token
        //get frame states on ledger
        val frameStateAndRef = serviceHub.vaultService.queryBy<FrameTokenState>().states
                .filter { it.state.data.serialNum.equals(frameSerial) }[0]

        //get the TokenType object
        val frametokentype = frameStateAndRef.state.data

        //get the pointer pointer to the frame
        val frametokenPointer: TokenPointer<*> = frametokentype.toPointer(frametokentype.javaClass)

        //Step 2: Wheels Token
        val wheelStateAndRef = serviceHub.vaultService.queryBy<WheelsTokenState>().states
                .filter { it.state.data.serialNum.equals(wheelsSerial) }[0]

        //get the TokenType object
        val wheeltokentype: WheelsTokenState = wheelStateAndRef.state.data

        //get the pointer pointer to the wheel
        val wheeltokenPointer: TokenPointer<*> = wheeltokentype.toPointer(wheeltokentype.javaClass)

        //send tokens
        val session = initiateFlow(holder)
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val txBuilder = TransactionBuilder(notary)
        addMoveNonFungibleTokens(txBuilder,serviceHub,frametokenPointer,holder)
        addMoveNonFungibleTokens(txBuilder,serviceHub,wheeltokenPointer,holder)
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))
        val ftx = subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))

        /* Distribution list is a list of identities that should receive updates. For this mechanism to behave correctly we call the UpdateDistributionListFlow flow */
        subFlow(UpdateDistributionListFlow(ftx))
        return ("\nTransfer ownership of a bike (Frame serial#: " + this.frameSerial + ", Wheels serial#: " + this.wheelsSerial + ") to "
                + holder.name.organisation + "\nTransaction IDs: "
                + ftx.id)
    }
}

@InitiatedBy(TransferBikeToken::class)
class TransferBikeTokenResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        subFlow(ObserverAwareFinalityFlowHandler(flowSession))
    }
}

