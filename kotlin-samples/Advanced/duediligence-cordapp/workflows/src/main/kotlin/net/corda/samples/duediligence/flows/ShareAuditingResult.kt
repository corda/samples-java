package net.corda.samples.duediligence.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow.Companion.tracker
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault.RelevancyStatus
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.duediligence.contracts.Commands
import net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import java.util.*

@InitiatingFlow
@StartableByRPC
class ShareAuditingResultInitiator(private val AuditingResultID: UniqueIdentifier, private val sendTo: Party, private val trustedAuditorAttachment: SecureHash) : FlowLogic<String?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {

        //Query the input
        val inputCriteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(AuditingResultID.toString())))
                .withStatus(StateStatus.UNCONSUMED)
                .withRelevancyStatus(RelevancyStatus.RELEVANT)
        val inputStateAndRef: StateAndRef<*> = serviceHub.vaultService.queryBy<CorporateRecordsAuditRequest>(CorporateRecordsAuditRequest::class.java, inputCriteria).states.get(0)
        val input = inputStateAndRef.state.data as CorporateRecordsAuditRequest

        //Send the copy to PartyB.
        val originalTx = serviceHub.validatedTransactions.getTransaction(inputStateAndRef.ref.txhash)

        //extract the notary
        val notary = inputStateAndRef.state.notary
        val copyId = UniqueIdentifier()
        val copyOfResult = CopyOfCoporateRecordsAuditRequest(
                qualification = input.qualification,
                originalOwner = input.applicant,
                copyReceiver = sendTo,
                originalRequestId = input.linearId,
                originalReportTxId = originalTx!!.id,
                originalValidater = input.validater,
                linearId = copyId)
        val txBuilder = TransactionBuilder(notary)
                .addReferenceState(inputStateAndRef.referenced())
                .addOutputState(copyOfResult)
                .addCommand(Commands.Share(), Arrays.asList(input.applicant.owningKey, sendTo.owningKey))
                .addAttachment(trustedAuditorAttachment)

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(sendTo)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), tracker()))

        // Notarise and record the transaction in both parties' vaults.
        subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)))
        return """
            A Copy of Corporate Auditing Report has sent to${sendTo.name.organisation}
            ID of the Copy: $copyId
            """.trimIndent()
    }

}

@InitiatedBy(ShareAuditingResultInitiator::class)
class ShareAuditingResultResponder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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
//flow start ShareAuditingResultInitiator AuditingResultID: 503ae520-bbf3-4729-9553-fbc407585064, sendTo: BankB, trustedAuditorAttachment: "8DF3275D80B26B9A45AB022F2FDA4A2ED996449B425F8F2245FA5BCF7D1AC587"