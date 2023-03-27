package net.corda.samples.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.issuedBy
import net.corda.samples.obligation.accountUtil.NewKeyForAccount
import java.util.*

@InitiatingFlow
@StartableByRPC
class MoneyDropFlow(val acctID: UUID) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val acctParty = subFlow(NewKeyForAccount(acctID)).party
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val issuerBankPartyRef = OpaqueBytes.of(0)
        val amount  = Random().nextInt(100)
        val issueAmount = Amount( 1+(amount.toLong()*100), Currency.getInstance("USD"))

        val builder = TransactionBuilder(notary)
        val issuer = ourIdentity.ref(issuerBankPartyRef)
        val signers = Cash().generateIssue(builder, issueAmount.issuedBy(issuer), acctParty, notary!!)
        val tx = serviceHub.signInitialTransaction(builder, signers)

        // There is no one to send the tx to as we're the only participants
        return subFlow(FinalityFlow(tx,listOf()))
    }
}

