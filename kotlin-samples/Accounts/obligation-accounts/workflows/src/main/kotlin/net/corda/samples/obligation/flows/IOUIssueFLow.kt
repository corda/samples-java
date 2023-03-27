package net.corda.samples.obligation.flows


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.samples.obligation.states.IOUState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.obligation.contract.IOUContract
import java.util.*

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUIssueFlow(
    val meID: UUID,
    val lenderID: UUID,
    val amount: Int
    ): FlowLogic<String>() {
    @Suspendable
    override fun call(): String {

        val borrowerAccount = accountService.accountInfo(meID)!!.state.data
        val borrowerKey = subFlow(RequestKeyForAccount(borrowerAccount)).owningKey

        val lenderAccount = accountService.accountInfo(lenderID)!!.state.data
        val lenderAccountParty = subFlow(RequestKeyForAccount(lenderAccount))

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        //might change
        val amount = Amount(amount.toLong() * 100, Currency.getInstance("USD"))

        val state = IOUState(amount,lenderAccountParty,AnonymousParty(borrowerKey),lenderID,ourIdentity)

        val builder = TransactionBuilder(notary = notary)
            .addOutputState(state, IOUContract.IOU_CONTRACT_ID)
            .addCommand(Command(IOUContract.Commands.Issue(), state.participants.map { it.owningKey }))

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder,listOfNotNull(borrowerKey))

        val sessionTolender = initiateFlow(lenderAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(ptx, sessionTolender, lenderAccountParty.owningKey))
        val signedByCounterParty = ptx.withAdditionalSignatures(accountToMoveToSignature)

        subFlow(FinalityFlow(signedByCounterParty, listOf(sessionTolender).filter { it.counterparty != ourIdentity }))
        subFlow(SyncIOU(state.linearId, lenderAccount.host))
        return state.linearId.id.toString()
    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUIssueFlow::class)
class IOUIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is IOUState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))

    }
}
