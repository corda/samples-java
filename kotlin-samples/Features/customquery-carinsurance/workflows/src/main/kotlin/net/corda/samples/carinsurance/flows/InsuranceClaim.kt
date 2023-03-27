package net.corda.samples.carinsurance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.Builder
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.carinsurance.contracts.InsuranceContract
import net.corda.samples.carinsurance.schema.InsuranceSchemaV1
import net.corda.samples.carinsurance.states.Claim
import net.corda.samples.carinsurance.states.InsuranceState
import java.lang.reflect.Field

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


        /*************************************************************************************
         * This section is the custom query code. Instead of query out all the insurance state and filter by their policy number,
         * custom query will only retrieve the insurance state that matched the policy number. The filtering will happen behind the scene.
         * **/
        val policyNumbercriteria = VaultCustomQueryCriteria(builder {
            InsuranceSchemaV1.PersistentInsurance::policyNumber.equal(policyNumber, false) })
        /** And you can have joint custom criteria as well. Simply add additional criteria and add it to the criteria object by using and().
          * val insuredValuecriteria = VaultCustomQueryCriteria(builder { InsuranceSchemaV1.PersistentInsurance::insuredValue.equal(insuredValue, false) })
          * **/
        var criteria= VaultQueryCriteria(StateStatus.UNCONSUMED).and(policyNumbercriteria)
                ///.and(insuredValuecriteria)
        val insuranceStateAndRefs = serviceHub.vaultService.queryBy(InsuranceState::class.java, criteria)
        /***************************************************************************************/

        if (insuranceStateAndRefs.states.isEmpty()) {
            throw IllegalArgumentException("Policy not found")
        }
        val inputStateAndRef = insuranceStateAndRefs.states[0]

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
