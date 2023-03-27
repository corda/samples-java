package net.corda.samples.statereissuance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.samples.statereissuance.contracts.LandTitleContract
import net.corda.samples.statereissuance.states.LandTitleState
import java.util.*


@InitiatingFlow
@StartableByRPC
class TransferLandTitle(private val plotIdentifier: UniqueIdentifier,
                            private val owner: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val landTitleStateAndRefs = serviceHub.vaultService.queryBy(LandTitleState::class.java).states

        val inputStateAndRef = landTitleStateAndRefs.stream().filter{ it.state.data.linearId == plotIdentifier }
                .findAny().orElseThrow{IllegalArgumentException("Land Title Not Found")}

        val inputState = inputStateAndRef.state.data

        val outputState = LandTitleState(inputState.linearId,inputState.dimensions,inputState.area, inputState.issuer,owner)

        val notary = inputStateAndRef.state.notary

        val builder = TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState)
                .addCommand(LandTitleContract.Commands.Transfer(), listOf(inputState.issuer.owningKey,inputState.owner.owningKey))

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val issuerSession = initiateFlow(inputState.issuer)
        issuerSession.send("signer")
        val newOwnerSession = initiateFlow(owner)
        newOwnerSession.send("not-signer")

        val stx = subFlow(CollectSignaturesFlow(ptx, Arrays.asList(issuerSession)))

        // Step 7. Assuming no exceptions, we can now finalise the transaction
        return subFlow<SignedTransaction>(FinalityFlow(stx, listOf(issuerSession,newOwnerSession)))
    }
}

@InitiatedBy(TransferLandTitle::class)
class TransferLandTitleResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val signRequired = counterpartySession.receive(String::class.java).unwrap { it }
        if(signRequired == "signer"){
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    //Addition checks
                }
            }
            subFlow(signTransactionFlow)
        }
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
