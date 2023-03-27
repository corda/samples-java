package net.corda.samples.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.samples.obligation.states.IOUState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash

import net.corda.finance.workflows.asset.CashUtils
import net.corda.samples.obligation.accountUtil.NewKeyForAccount
import net.corda.samples.obligation.contract.IOUContract
import java.util.*

/**
 * This is the flow which handles the (partial) settlement of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled vy the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@InitiatingFlow
@StartableByRPC
class IOUSettleFlow(
    val linearId: UniqueIdentifier,
    val meID: UUID,
    val settleAmount: Int): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // Step 1. Retrieve the IOU state from the vault.
        val amount = Amount( settleAmount.toLong()*100, Currency.getInstance("USD"))


        val myAccount = accountService.accountInfo(meID)!!.state.data
        val myAcctPartyAndCert = subFlow(NewKeyForAccount(myAccount.identifier.id))
        val criteria = QueryCriteria.VaultQueryCriteria(
            externalIds = listOf(myAccount.identifier.id)
        )
        val iouToSettle = serviceHub.vaultService.queryBy(
            contractStateType = IOUState::class.java,
            criteria = criteria
        ).states.filter {it.state.data.linearId == linearId}[0]

        //val counterparty = iouToSettle.state.data.lender
        val lenderId = iouToSettle.state.data.lenderAcctID
        val targetAccount = accountService.accountInfo(lenderId)!!.state.data
        val counterparty = subFlow(RequestKeyForAccount(targetAccount))


        // Step 2. Check the party running this flow is the borrower.
        val borrowerAccount = accountService.accountInfo(meID)!!.state.data
        if (ourIdentity != borrowerAccount.host) {
            throw IllegalArgumentException("IOU settlement flow must be initiated by the borrower.")
        }

        // Step 3. Create a transaction builder.
        val notary = iouToSettle.state.notary
        val builder = TransactionBuilder(notary = notary)

        // Step 4. Check we have enough cash to settle the requested amount.
        var cashBalance:Long = 0
        serviceHub.vaultService.queryBy(
            contractStateType = Cash.State::class.java,
            criteria = criteria
        ).states.map {
            cashBalance += it.state.data.amount.quantity
            "\n" + "cash balance: " + it.state.data.amount
        }

        if (cashBalance < amount.quantity) {
            throw IllegalArgumentException("Borrower has only $cashBalance but needs $amount to settle.")
        } else if (amount > (iouToSettle.state.data.amount - iouToSettle.state.data.paid)) {
            throw IllegalArgumentException("Borrower tried to settle with $amount but only needs ${ (iouToSettle.state.data.amount - iouToSettle.state.data.paid) }")
        }

        // Step 5. Get some cash from the vault and add a spend to our transaction builder.
        // Vault might contain states "owned" by anonymous parties. This is one of techniques to anonymize transactions
        // generateSpend returns all public keys which have to be used to sign transaction
        val (_, cashKeys) = CashUtils.generateSpend(serviceHub, builder, amount, myAcctPartyAndCert, counterparty, anonymous = false)

        // Step 6. Add the IOU input state and settle command to the transaction builder.
        val settleCommand = Command(IOUContract.Commands.Settle(), listOf(counterparty.owningKey, myAcctPartyAndCert.owningKey))
        // Add the input IOU and IOU settle command.
        builder.addCommand(settleCommand)
        builder.addInputState(iouToSettle)

        // Step 7. Only add an output IOU state of the IOU has not been fully settled.
        val amountRemaining = iouToSettle.state.data.amount - iouToSettle.state.data.paid - amount
        if (amountRemaining > Amount(0, amount.token)) {
            val settledIOU: IOUState = iouToSettle.state.data.pay(amount)
            builder.addOutputState(settledIOU, IOUContract.IOU_CONTRACT_ID)
        }

        // Step 8. Verify and sign the transaction.
        builder.verify(serviceHub)
        // We need to sign transaction with all keys referred from Cash input states + our public key
        val myKeysToSign = (cashKeys.toSet() + myAcctPartyAndCert.owningKey).toList()
        val ptx = serviceHub.signInitialTransaction(builder, myKeysToSign)


        val sessionTolender = initiateFlow(targetAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(ptx, sessionTolender, counterparty.owningKey))
        val signedByCounterParty = ptx.withAdditionalSignatures(accountToMoveToSignature)

        // Step 10. Finalize the transaction.
        return subFlow(FinalityFlow(signedByCounterParty, sessionTolender))

    }
}

/**
 * This is the flow which signs IOU settlements.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(IOUSettleFlow::class)
class IOUSettleFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // signing transaction
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) {
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}
