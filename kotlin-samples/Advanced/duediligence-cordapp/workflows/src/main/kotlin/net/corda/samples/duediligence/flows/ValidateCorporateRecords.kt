package net.corda.samples.duediligence.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow.Companion.tracker
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault.RelevancyStatus
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.duediligence.contracts.Commands
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import java.util.*

@InitiatingFlow
@StartableByRPC
class ValidateCorporateRecordsInitiator(private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {

        //Query the input
        val inputCriteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(linearId.toString())))
                .withStatus(StateStatus.UNCONSUMED)
                .withRelevancyStatus(RelevancyStatus.RELEVANT)
        val inputStateAndRef: StateAndRef<*> = serviceHub.vaultService.queryBy<CorporateRecordsAuditRequest>(CorporateRecordsAuditRequest::class.java, inputCriteria).states.get(0)
        val input = inputStateAndRef.state.data as CorporateRecordsAuditRequest

        //extract the notary
        val notary = inputStateAndRef.state.notary

        //Creating the output
        val output = CorporateRecordsAuditRequest(
                applicant = input.applicant,
                validater = ourIdentity,
                numberOfFiles = input.numberOfFiles,
                linearId = input.linearId)

        //set validation status to true
        output.validatedAndApproved()

        //Build transaction
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(output)
                .addCommand(Commands.Validate(),
                        Arrays.asList(ourIdentity.owningKey, input.applicant.owningKey))

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(input.applicant)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), tracker()))

        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)))
    }
}

@InitiatedBy(ValidateCorporateRecordsInitiator::class)
class ValidateCorporateRecordsResponder(
        private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val signedTransaction = subFlow(object : SignTransactionFlow(counterpartySession) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        })
        //Stored the transaction into data base.
        return subFlow(ReceiveFinalityFlow(counterpartySession, signedTransaction.id))
    }
}

//flow start RequestToValidateCorporateRecordsInitiator validater: PartyB, numberOfFiles: 10
