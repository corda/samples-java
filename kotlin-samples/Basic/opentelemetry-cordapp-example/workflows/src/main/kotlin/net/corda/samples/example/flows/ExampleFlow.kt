package net.corda.samples.example.flows

import co.paralleluniverse.fibers.Suspendable
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import net.corda.samples.example.flows.ExampleFlow.Acceptor
import net.corda.samples.example.flows.ExampleFlow.Initiator
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.samples.example.contracts.IOUContract
import net.corda.samples.example.states.IOUState
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope
import net.corda.core.node.ServiceHub
import java.util.*

data class OpenTelemetrySpan(val span: Span?,
                             val scope: Scope?,
                             val baggageScope: Scope?)

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object ExampleFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val iouValue: Int,
                    val otherParty: Party) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )

            // Note that Spans cannot be included in a checkpoint, if you attempt to include one an exception will
            // occur. So we create a map of UUID to Span and have the UUID included in the checkpoint. And then use
            // this UUID when clearing down the spans.
            private val spansMap = mutableMapOf<UUID, OpenTelemetrySpan>()

            // Create a tracer from the open telemetry handle given to us from Corda. Note that getTelemetryHandle(..)
            // is the only Corda open telemetry related operation here. All other open telemetry calls are using the
            // open telemetry api.
            private fun getTracer(serviceHub: ServiceHub) =
                serviceHub.telemetryService.getTelemetryHandle(OpenTelemetry::class.java)?.getTracer("ExampleFlow")

            private fun createSpanAndSampleBaggage(serviceHub: ServiceHub, spanName: String, baggageValue: String): UUID {
                val span = getTracer(serviceHub)?.spanBuilder(spanName)?.startSpan()
                val scope = span?.makeCurrent()
                val myBaggage: Map<String, String>? = mapOf("baggage.from.my.flow" to baggageValue)
                val baggage = myBaggage?.toList()?.fold(Baggage.current().toBuilder()) { builder, attribute ->
                    builder.put(
                        attribute.first,
                        attribute.second
                    )
                }
                var baggageScope: Scope? = baggage?.build()?.makeCurrent()
                return addToSpanMap(OpenTelemetrySpan(span, scope, baggageScope))
            }

            private fun closeSpanAndBaggage(uuid: UUID) {
                val spanInfo = spansMap[uuid]
                spanInfo?.baggageScope?.close()
                spanInfo?.scope?.close()
                spanInfo?.span?.end()
            }

            private fun addToSpanMap(spanInfo: OpenTelemetrySpan): UUID {
                val uuid = UUID.randomUUID()
                spansMap[uuid] = spanInfo
                return uuid
            }
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {

            // This makes sure only the spanUuid will be included in a checkpoint. See comment above.
            val spanUuid = createSpanAndSampleBaggage(serviceHub, "my span from flow", "baggage from my flow - ${UUID.randomUUID()}")
            try {

                // Obtain a reference from a notary we wish to use.
                /**
                 *  METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
                 *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
                 *
                 *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
                 */
                val notary = serviceHub.networkMapCache.notaryIdentities.single() // METHOD 1
                // val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

                // Stage 1.
                progressTracker.currentStep = GENERATING_TRANSACTION
                // Generate an unsigned transaction.
                val iouState = IOUState(iouValue, serviceHub.myInfo.legalIdentities.first(), otherParty)
                val txCommand = Command(IOUContract.Commands.Create(), iouState.participants.map { it.owningKey })
                val txBuilder = TransactionBuilder(notary)
                    .addOutputState(iouState, IOUContract.ID)
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
                    CollectSignaturesFlow(
                        partSignedTx,
                        setOf(otherPartySession),
                        GATHERING_SIGS.childProgressTracker()
                    )
                )

                // Stage 5.
                progressTracker.currentStep = FINALISING_TRANSACTION
                // Notarise and record the transaction in both parties' vaults.
                return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
            }
            finally {
                closeSpanAndBaggage(spanUuid)
            }
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction." using (output is IOUState)
                    val iou = output as IOUState
                    "I won't accept IOUs with a value over 100." using (iou.value <= 100)
                }
            }
            val txId = subFlow(signTransactionFlow).id

            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}
