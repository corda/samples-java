package net.corda.samples.notarychange.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.notarychange.contracts.IOUContract
import net.corda.samples.notarychange.states.IOUState
import java.util.*

class SettleFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator : FlowLogic<SignedTransaction> {
        private var linearId: UniqueIdentifier
        private var notary: Party?
        private val QUERYING_VAULT = ProgressTracker.Step("Fetching IOU from node's vault.")
        private val GENERATING_TRANSACTION = ProgressTracker.Step("Generating transaction based on new IOU.")
        private val VERIFYING_TRANSACTION = ProgressTracker.Step("Verifying contract constraints.")
        private val SIGNING_TRANSACTION = ProgressTracker.Step("Signing transaction with our private key.")
        private val GATHERING_SIGS: ProgressTracker.Step = object : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker(): ProgressTracker? {
                return CollectSignaturesFlow.tracker()
            }
        }
        private val FINALISING_TRANSACTION: ProgressTracker.Step = object : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker(): ProgressTracker? {
                return FinalityFlow.tracker()
            }
        }

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        override val progressTracker = ProgressTracker(
                QUERYING_VAULT,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )

        // Constructor
        constructor(linearId: UniqueIdentifier) {
            this.linearId = linearId
            notary = null
        }

        // Constructor used to allow user to select notary of choice
        constructor(linearId: UniqueIdentifier, notary: Party?) {
            this.linearId = linearId
            this.notary = notary
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            progressTracker.currentStep = QUERYING_VAULT
            val queryCriteria: QueryCriteria = LinearStateQueryCriteria(null, listOf(linearId.id))
            val (states) = serviceHub.vaultService.queryBy(IOUState::class.java, queryCriteria)
            if (states.size == 0) {
                throw FlowException("No IOU found for LinearId:$linearId")
            }
            val iouStateStateAndRef = states[0]
            val inputStateToSettle = iouStateStateAndRef.state.data
            if (inputStateToSettle.borrower.owningKey != ourIdentity.owningKey) {
                throw FlowException("The borrower must initiate the flow")
            }
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val me = ourIdentity
            val txCommand = Command(
                    IOUContract.Commands.Settle(),
                    Arrays.asList(inputStateToSettle.lender.owningKey, inputStateToSettle.borrower.owningKey))
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(iouStateStateAndRef)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val otherPartySession = initiateFlow(inputStateToSettle.lender)
            val fullySignedTx = subFlow(
                    CollectSignaturesFlow(partSignedTx, listOf(otherPartySession), CollectSignaturesFlow.tracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, listOf(otherPartySession)))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(private val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            subFlow(object : SignTransactionFlow(otherPartySession) {
                @Suspendable
                @Throws(FlowException::class)
                override fun checkTransaction(stx: SignedTransaction) {
                    // Implement responder flow transaction checks here
                }
            })
            return subFlow(ReceiveFinalityFlow(otherPartySession))
        }
    }
}