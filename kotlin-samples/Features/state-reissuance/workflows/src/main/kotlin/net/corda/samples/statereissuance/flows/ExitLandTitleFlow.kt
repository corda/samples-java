package net.corda.samples.statereissuance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.statereissuance.contracts.LandTitleContract
import net.corda.samples.statereissuance.states.LandTitleState

@InitiatingFlow
@StartableByRPC
class ExitLandTitle(private val stateRef: StateRef) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val landTitleStateAndRefs = serviceHub.vaultService.queryBy(LandTitleState::class.java).states

        val inputStateAndRef = landTitleStateAndRefs.stream().filter{ it.ref == stateRef }
                .findAny().orElseThrow{IllegalArgumentException("Land Title Not Found")}

        val inputState = inputStateAndRef.state.data
        val notary = inputStateAndRef.state.notary

        val builder = TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addCommand(LandTitleContract.Commands.Exit(), listOf(inputState.issuer.owningKey, ourIdentity.owningKey))

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val issuerSession = initiateFlow(inputState.issuer)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(issuerSession)))
        return subFlow(FinalityFlow(stx, listOf(issuerSession)))
    }
}

@InitiatedBy(ExitLandTitle::class)
class ExitLandTitleResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                //Addition checks
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}
