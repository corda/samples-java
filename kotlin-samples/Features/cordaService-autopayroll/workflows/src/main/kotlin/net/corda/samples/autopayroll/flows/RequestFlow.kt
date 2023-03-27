package net.corda.samples.autopayroll.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.samples.autopayroll.contracts.PaymentRequestContract
import net.corda.samples.autopayroll.states.PaymentRequestState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class RequestFlowInitiator(
        private val amount:String,
        private val towhom:Party
) : FlowLogic<SignedTransaction>() {
    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction for between accounts")
        object PROCESSING_TRANSACTION : ProgressTracker.Step("PROCESS transaction with our private key.")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")

        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                PROCESSING_TRANSACTION,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction{

        progressTracker.currentStep = GENERATING_TRANSACTION

        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

        val bank = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name("BankOperator", "Toronto", "CA"))!!
        val output = PaymentRequestState(amount, towhom, participants = listOf(ourIdentity, bank))
        val transactionBuilder = TransactionBuilder(notary)
        val commandData = PaymentRequestContract.Commands.Request()
        transactionBuilder.addCommand(commandData, ourIdentity.owningKey, bank.owningKey)
        transactionBuilder.addOutputState(output, PaymentRequestContract.ID)
        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = PROCESSING_TRANSACTION
        val session = initiateFlow(bank)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        progressTracker.currentStep = FINALISING_TRANSACTION
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))
        return subFlow(FinalityFlow(fullySignedTransaction, listOf(session)))

    }
}

@InitiatedBy(RequestFlowInitiator::class)
class RequestFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                if (stx.inputs.isNotEmpty()) {
                    throw FlowException("Payment Request should not have inputs.")
                }
            }
        }
        val txId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))

    }
}
