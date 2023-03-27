package net.corda.samples.duediligence.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.ContractState
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
import net.corda.samples.duediligence.contracts.DueDChecklistContract
import net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import net.corda.samples.duediligence.states.DueDChecklist
import java.security.PublicKey
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateCheckListAndAddApprovalInitiator(private val reportTo: Party, private val approvalId: UniqueIdentifier) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {

        //Query the input
        val inputCriteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(approvalId.toString())))
                .withStatus(StateStatus.UNCONSUMED)
                .withRelevancyStatus(RelevancyStatus.RELEVANT)
        val inputStateAndRef: StateAndRef<*> = serviceHub.vaultService.queryBy<ContractState>(ContractState::class.java, inputCriteria).states.get(0)

        //extract the notary
        val notary = inputStateAndRef.state.notary


        //create due-diligence Checklist
        val checklist = DueDChecklist(numberOfapprovalsNeeded = 3,
                operationNode = ourIdentity,
                reportTo = reportTo,
                linearId = UniqueIdentifier())
        checklist.uploadApproval(approvalId)
        val txBuilder: TransactionBuilder = TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(checklist)
                .addCommand(DueDChecklistContract.Commands.Add(), Arrays.asList(ourIdentity.owningKey, reportTo.owningKey))

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(reportTo)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), tracker()))

        // Notarise and record the transaction in both parties' vaults.
        return if (inputStateAndRef.state.data.javaClass == CorporateRecordsAuditRequest::class.java) {
            val request = inputStateAndRef.state.data as CorporateRecordsAuditRequest
            subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession, initiateFlow(request.validater))))
        } else {
            val request = inputStateAndRef.state.data as CopyOfCoporateRecordsAuditRequest
            subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession, initiateFlow(request.originalValidater), initiateFlow(request.originalOwner))))
        }
    }
}

@InitiatedBy(CreateCheckListAndAddApprovalInitiator::class)
class CreateCheckListAndAddApprovalResponder
(
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
