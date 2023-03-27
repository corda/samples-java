package net.corda.samples.secretsanta.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.istack.Nullable
import net.corda.core.contracts.CommandData
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.secretsanta.contracts.SantaSessionContract
import net.corda.samples.secretsanta.states.SantaSessionState


/**
 * santaPlayers is a list of Pairs representing the names and emails of the players in this session
 */
@StartableByRPC
@InitiatingFlow
class CreateSantaSessionFlow(
        private val playerNames: List<String?>,
        private val playerEmails: List<String>,
        private val owner: Party) : FlowLogic<SignedTransaction>() {

    override var progressTracker = ProgressTracker(
            CREATING,
            SIGNING,
            VERIFYING,
            FINALISING,
            FINALDISPLAY
    )

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction { // run an issuance transaction for a new secret santa game
        progressTracker.currentStep = CREATING
        //notary
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val issuer = ourIdentity
        // elves 'own' a secret santa session
        val newSantaState = SantaSessionState(playerNames, playerEmails, issuer, owner)
        val transactionBuilder = TransactionBuilder(notary)
        val commandData: CommandData = SantaSessionContract.Commands.Issue()
        transactionBuilder.addCommand(commandData, issuer.owningKey, owner.owningKey)
        transactionBuilder.addOutputState(newSantaState, SantaSessionContract.ID)
        progressTracker.currentStep = VERIFYING
        transactionBuilder.verify(serviceHub)
        val session = initiateFlow(owner)
        progressTracker.currentStep = SIGNING
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        progressTracker.currentStep = FINALISING
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))
        progressTracker.currentStep = FINALDISPLAY
        return subFlow(FinalityFlow(fullySignedTransaction, listOf(session)))
    }

    companion object {
        private val CREATING = ProgressTracker.Step("Shoveling coal in the server . . .")
        private val SIGNING = ProgressTracker.Step("Getting the message to Santa . . .")
        private val VERIFYING = ProgressTracker.Step("Gathering the reindeer . . .")
        private val FINALISING = ProgressTracker.Step("Sending christmas cheer!")
        private val FINALDISPLAY: ProgressTracker.Step = object : ProgressTracker.Step("Secret Santa has been successfully generated.") {
            @Nullable
            override fun childProgressTracker(): ProgressTracker? {
                return FinalityFlow.tracker()
            }
        }
    }

}

@InitiatedBy(CreateSantaSessionFlow::class)
class CreateSantaSessionFlowResponder(private val otherSide: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransaction = subFlow(object : SignTransactionFlow(otherSide) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) { // Implement responder flow transaction checks here
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSide, signedTransaction.id))
    }
}
