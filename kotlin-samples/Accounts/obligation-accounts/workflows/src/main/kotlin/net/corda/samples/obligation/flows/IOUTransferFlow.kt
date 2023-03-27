package net.corda.samples.obligation.flows


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfoFlow
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.obligation.contract.IOUContract
import net.corda.samples.obligation.states.IOUState
import java.util.*


/**
 * This is the flow which handles transfers of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUTransferFlow(val linearId: UniqueIdentifier,
                      val meID: UUID,
                      val newLenderID: UUID): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // get the input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val iouStateAndRef =  serviceHub.vaultService.queryBy<IOUState>(queryCriteria).states.single()
        val inputIou = iouStateAndRef.state.data

        //check if the initiator is the lender of the input IOU
        if (meID != inputIou.lenderAcctID) {
            throw IllegalArgumentException("IOU transfer can only be initiated by the IOU lender. $meID ")
        }

        //generate 3 new key pairs
        val borrowerAccount = accountService.accountInfo(inputIou.borrower.owningKey)!!.state.data
        val borrowerAcctAnonymousParty = subFlow(RequestKeyForAccount(borrowerAccount))

        val oldLenderAccount = accountService.accountInfo(meID)!!.state.data
        val oldLenderAcctPartyAndCert = subFlow(RequestKeyForAccount(oldLenderAccount))

        val newLenderAccount = accountService.accountInfo(newLenderID)!!.state.data
        val newLenderAcctAnonymousParty = subFlow(RequestKeyForAccount(newLenderAccount))

        // Build IOU output state, update participants list
        val outputIou = inputIou.withNewLender(newLenderAcctAnonymousParty, newLenderID)

        //add signers and build command
        val signers =  listOf(
            borrowerAcctAnonymousParty.owningKey,
            newLenderAcctAnonymousParty.owningKey,
            oldLenderAcctPartyAndCert.owningKey)
        val transferCommand = Command(IOUContract.Commands.Transfer(), signers)

        //build tx builder
        val notary = iouStateAndRef.state.notary
        val builder = TransactionBuilder(notary = notary)
        builder.withItems(
            iouStateAndRef,
            StateAndContract(outputIou, IOUContract.IOU_CONTRACT_ID),
            transferCommand)

        //Self verify
        builder.verify(serviceHub)

        //self sign with original lender's newly generated key
        val locallySignedTx = serviceHub.signInitialTransaction(builder,listOf(ourIdentity.owningKey,oldLenderAcctPartyAndCert.owningKey))

        val sessions = listOf(newLenderAccount.host, inputIou.borrowerHost).map {initiateFlow(it)}.toSet()
        val signedByALLParty = subFlow(CollectSignaturesFlow(locallySignedTx,sessions))
        return subFlow(FinalityFlow(signedByALLParty, sessions))

        /* The following code is to show how to collect signature manually one party at the time. */
//        //session goes to the new lender acct's parent node
//        val newLenderSession = initiateFlow(newLenderAccount.host)
//        val lenderSignature = subFlow(CollectSignatureFlow(locallySignedTx, newLenderSession, newLenderAcctAnonymousParty.owningKey))
//        val signedByNewLender = locallySignedTx.withAdditionalSignatures(lenderSignature)
//
//        //session goes to the borrower acct's parent node
//        val borrowerSession = initiateFlow(inputIou.borrowerHost)
//        val borrowerSignature = subFlow(CollectSignatureFlow(signedByNewLender, borrowerSession, borrowerAcctAnonymousParty.owningKey))
//        val signedByALLParty = signedByNewLender.withAdditionalSignatures(borrowerSignature)
//        return subFlow(FinalityFlow(signedByALLParty, listOf(newLenderSession,borrowerSession)))
    }
}

/**
 * This is the flow which signs IOU transfers.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUTransferFlow::class)
class IOUTransferFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
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