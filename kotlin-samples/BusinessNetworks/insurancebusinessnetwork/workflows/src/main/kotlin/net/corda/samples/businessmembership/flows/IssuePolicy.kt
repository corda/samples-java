package net.corda.samples.businessmembership.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.businessmembership.contracts.InsuranceStateContract
import net.corda.samples.businessmembership.flows.membershipFlows.BusinessNetworkIntegrationFlow
import net.corda.samples.businessmembership.states.InsuranceState


@InitiatingFlow
@StartableByRPC
class IssuePolicy(
        private val networkId: String,
        private val careProvider: Party,
        private val insuree: String
) : BusinessNetworkIntegrationFlow<SignedTransaction>()  {

    @Suspendable
    override fun call(): SignedTransaction {
        businessNetworkFullVerification(networkId, ourIdentity, careProvider)
        val outputState = InsuranceState(insurer = ourIdentity, insuree = insuree,
                networkId = networkId, policyStatus = "Initiating Policy", careProvider = careProvider)

        val (insurerMembership, CareProviderMembership) = businessNetworkPartialVerification(networkId, ourIdentity, careProvider)

        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val builder = TransactionBuilder(notary)
                .addOutputState(outputState)
                .addCommand(InsuranceStateContract.Commands.Issue(), ourIdentity.owningKey, careProvider.owningKey)
                .addReferenceState(ReferencedStateAndRef(insurerMembership))
                .addReferenceState(ReferencedStateAndRef(CareProviderMembership))
        builder.verify(serviceHub)

        val selfSignedTransaction = serviceHub.signInitialTransaction(builder)
        val sessions = listOf(initiateFlow(careProvider))
        val fullSignedTransaction = subFlow(CollectSignaturesFlow(selfSignedTransaction, sessions))
        return subFlow(FinalityFlow(fullSignedTransaction, sessions))
    }
}

@InitiatedBy(IssuePolicy::class)
class IssueLoanResponderFlow(private val session: FlowSession) : BusinessNetworkIntegrationFlow<Unit>() {

    @Suspendable
    override fun call() {
        val signResponder = object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) {
                val command = stx.tx.commands.single()
                if (command.value !is InsuranceStateContract.Commands.Issue) {
                    throw FlowException("Only LoanContract.Commands.Issue command is allowed")
                }

                val insuranceState = stx.tx.outputStates.single() as InsuranceState
                insuranceState.apply {
                    if (insurer != session.counterparty) {
                        throw FlowException("insurer doesn't match sender's identity")
                    }
                    if (careProvider != ourIdentity) {
                        throw FlowException("careProvider doesn't match receiver's identity")
                    }
                    //businessNetworkFullVerification(networkId, insurer, careProvider)
                }
            }
        }
        val stx = subFlow(signResponder)
        subFlow(ReceiveFinalityFlow(session, stx.id))
    }
}