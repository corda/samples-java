package net.corda.samples.notarychange.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.notarychange.contracts.IOUContract
import net.corda.samples.notarychange.states.IOUState
import java.util.*

class IssueFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator : FlowLogic<String> {
        private val iouValue: Int
        private val otherParty: Party
        private var notary: Party?
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
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )

        // Constructor
        constructor(iouValue: Int, otherParty: Party) {
            this.iouValue = iouValue
            this.otherParty = otherParty
            notary = null
        }

        // Constructor used to allow user to select notary of choice
        constructor(iouValue: Int, otherParty: Party, notary: Party?) {
            this.iouValue = iouValue
            this.otherParty = otherParty
            this.notary = notary
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): String {

            // Obtain a reference to a notary we wish to use.
            if (notary == null)
                notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=NotaryA,L=London,C=GB")) // METHOD 2

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val me = ourIdentity
            val iouState = IOUState(iouValue, me, otherParty, UniqueIdentifier())
            val txCommand = Command(
                    IOUContract.Commands.Create(),
                    Arrays.asList(iouState.lender.owningKey, iouState.borrower.owningKey))
            val txBuilder = TransactionBuilder(notary!!)
                    .addOutputState(iouState)
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
            val otherPartySession = initiateFlow(otherParty)
            val fullySignedTx = subFlow(
                    CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.tracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession))).toString() + ", IOU created with linearId: " + iouState.linearId.toString()
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