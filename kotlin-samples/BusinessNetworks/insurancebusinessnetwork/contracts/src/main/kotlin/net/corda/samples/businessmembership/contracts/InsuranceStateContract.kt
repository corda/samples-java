package net.corda.samples.businessmembership.contracts

import net.corda.bn.states.MembershipState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.businessmembership.states.CareProviderIdentity
import net.corda.samples.businessmembership.states.InsuranceState
import net.corda.samples.businessmembership.states.InsurerIdentity
import java.lang.IllegalArgumentException

class InsuranceStateContract : Contract {

    companion object {
        const val CONTRACT_NAME = "net.corda.samples.businessmembership.contracts.InsuranceStateContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = if (tx.outputStates.isNotEmpty()) tx.outputs.single() else null
        val outputState = output?.data as? InsuranceState

        when (command.value) {
            is Commands.Issue -> verifyIssue(tx,outputState!!.networkId, outputState.insurer, outputState.careProvider)
            else -> throw IllegalArgumentException("Unsupported command ${command.value}")
        }
    }

    private fun verifyIssue(tx: LedgerTransaction, networkId: String, insurance: Party, CareProvider: Party) {
        verifyMembershipsForMedInsuranceTransaction(tx, networkId, insurance, CareProvider, "Issue")
    }


    /**
     * Contract verification check over reference [MembershipState]s.
     * Make sure the participants are in the correct [Network], and has the correct [CustomIdentityType]
     *
     * @param tx Ledger transaction over which contract performs verification.
     * @param lender Party issuing the loan.
     * @param borrower Party paying of the loan.
     */
    private fun verifyMembershipsForMedInsuranceTransaction(
            tx: LedgerTransaction,
            networkId: String,
            insurance: Party,
            CareProvider: Party,
            commandName: String) = requireThat {
        //Verify number of memberships
        "Insurance $commandName transaction should have 2 reference states" using (tx.referenceStates.size == 2)
        "Insurance $commandName transaction should contain only reference MembershipStates" using (tx.referenceStates.all { it is MembershipState })

        //Extract memberships
        val membershipReferenceStates = tx.referenceStates.map { it as MembershipState }

        //Check for membership network IDs.
        "Insurance $commandName transaction should contain only reference membership states from Business Network with $networkId ID" using (membershipReferenceStates.all { it.networkId == networkId })

        //Extract Membership and verify not null
        val insuranceMembership = membershipReferenceStates.find { it.networkId == networkId && it.identity.cordaIdentity == insurance }
        val careProviderMembership = membershipReferenceStates.find { it.networkId == networkId && it.identity.cordaIdentity == CareProvider }
        "Insurance $commandName transaction should have insurance's reference membership state" using (insuranceMembership != null)
        "Insurance $commandName transaction should have careProvider's reference membership state" using (careProviderMembership != null)

        //Exam the customized Identity
        insuranceMembership?.apply {
            "insurance should be active member of Business Network with $networkId" using (isActive())
            "insurance should have business identity of FirmIdentity type" using (identity.businessIdentity is InsurerIdentity)
        }
        careProviderMembership?.apply {
            "careProvider should be active member of Business Network with $networkId" using (isActive())
            "careProvider should have business identity of FirmIdentity type" using (identity.businessIdentity is CareProviderIdentity)
        }
    }

    /**
     * Each new [InsuranceStateContract] command must be wrapped and extend this class.
     */
    open class Commands : TypeOnlyCommandData() {
        /**
         * Command responsible for [InsuranceClaim] issuance.
         */
        class Issue : Commands()

        /**
         * Command responsible for [InsuranceClaim] partial settlement.
         */
        class Claim : Commands()

    }
}