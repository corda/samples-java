package net.corda.samples.carinsurance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.carinsurance.contracts.InsuranceContract
import net.corda.samples.carinsurance.states.Claim
import net.corda.samples.carinsurance.states.InsuranceState

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class InsuranceClaim(val claimInfo: ClaimInfo,
                     val policyNumber: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // Query the vault to fetch a list of all Insurance state, and filter the results based on the policyNumber
        // to fetch the desired Insurance state from the vault. This filtered state would be used as input to the
        // transaction.
        val insuranceStateAndRefs = serviceHub.vaultService.queryBy<InsuranceState>().states
        val inputStateAndRef = insuranceStateAndRefs.filter { it.state.data.policyNumber.equals(policyNumber) }[0]

        //compose claim
        val claim = Claim(claimInfo.claimNumber, claimInfo.claimDescription, claimInfo.claimAmount)
        val input = inputStateAndRef.state.data
        var claimlist = ArrayList<Claim>()
        claimlist.add(claim)
        for (item in input.claims) {
            claimlist.add(item)
        }

        //create the output
        val output = input.copy(claims = claimlist)

        // Build the transaction.
        val txBuilder = TransactionBuilder(inputStateAndRef.state.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(output)
                .addCommand(InsuranceContract.Commands.AddClaim(), listOf(ourIdentity.owningKey))

        // Verify the transaction
        txBuilder.verify(serviceHub)

        // Sign the transaction
        val stx = serviceHub.signInitialTransaction(txBuilder)

        val counterpartySession = initiateFlow(input.insuree)
        return subFlow(FinalityFlow(stx, listOf(counterpartySession)))


    }
}

@InitiatedBy(InsuranceClaim::class)
class InsuranceClaimResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        })
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

@CordaSerializable
class ClaimInfo(val claimNumber: String, val claimDescription: String, val claimAmount: Int)
