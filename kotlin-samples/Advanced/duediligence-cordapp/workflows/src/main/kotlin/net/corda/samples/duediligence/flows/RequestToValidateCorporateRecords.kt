package net.corda.samples.duediligence.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.flows.CollectSignaturesFlow.Companion.tracker
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.duediligence.contracts.Commands
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import java.util.*

@InitiatingFlow
@StartableByRPC
class RequestToValidateCorporateRecordsInitiator(
        private val validater: Party,
        private val numberOfFiles: Int
) : FlowLogic<String?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {

        //notary
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        //Initiate Corporate Records validation
        val cr = CorporateRecordsAuditRequest(
                applicant = ourIdentity,
                validater = validater,
                numberOfFiles = numberOfFiles)

        //Build transaction
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(cr)
                .addCommand(Commands.Propose(), Arrays.asList(ourIdentity.owningKey, validater.owningKey))

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(validater)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), tracker()))

        // Notarise and record the transaction in both parties' vaults.
        subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)))
        return """
            Corporate Records Auditing Request has sent to: ${cr.validater.name.organisation}
            Case Id: ${cr.linearId}
            """.trimIndent()
    }
}


@InitiatedBy(RequestToValidateCorporateRecordsInitiator::class)
class RequestToValidateCorporateRecordsResponder(private val counterpartySession: FlowSession)
    : FlowLogic<SignedTransaction>() {
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