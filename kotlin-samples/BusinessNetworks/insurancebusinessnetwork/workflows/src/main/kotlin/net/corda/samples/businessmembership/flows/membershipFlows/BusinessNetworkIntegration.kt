package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.BNService
import net.corda.bn.flows.IllegalMembershipStatusException
import net.corda.bn.flows.MembershipAuthorisationException
import net.corda.bn.flows.MembershipNotFoundException
import net.corda.bn.states.MembershipState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.samples.businessmembership.states.CareProviderIdentity
import net.corda.samples.businessmembership.states.InsurerIdentity
import net.corda.samples.businessmembership.states.IssuePermissions
import java.lang.IllegalStateException


data class Memberships(val MembershipA: StateAndRef<MembershipState>, val MembershipB: StateAndRef<MembershipState>)

/**
 * This abstract flow is extended by any flow which will use business network membership verification methods.
 */
abstract class BusinessNetworkIntegrationFlow<T> : FlowLogic<T>() {

    @Suspendable
    protected fun businessNetworkPartialVerification(networkId: String, insurer: Party, careProvider: Party): Memberships {
        val bnService = serviceHub.cordaService(BNService::class.java)
        val insurerMembership = bnService.getMembership(networkId, insurer)
                ?: throw MembershipNotFoundException("insurer is not part of Business Network with $networkId ID")
        val careProMembership = bnService.getMembership(networkId, careProvider)
                ?: throw MembershipNotFoundException("careProvider is not part of Business Network with $networkId ID")

        return Memberships(insurerMembership, careProMembership)
    }

    /**
     * Verifies that [lender] and [borrower] are members of Business Network with [networkId] ID, their memberships are active, contain
     * business identity of [BankIdentity] type and that lender is authorised to issue the loan.
     *
     * @param networkId ID of the Business Network in which verification is performed.
     * @param lender Party issuing the loan.
     * @param borrower Party paying of the loan.
     */
    @Suppress("ComplexMethod", "ThrowsCount")
    @Suspendable
    protected fun businessNetworkFullVerification(networkId: String, policyIssuer: Party, careProvider: Party) {
        val bnService = serviceHub.cordaService(BNService::class.java)

        // we put this in try catch block since lender is the caller of Business Network Service methods and those throw
        // [IllegalStateException] whenever the caller is not member of the Business Network.
        try {
            bnService.getMembership(networkId, policyIssuer)?.state?.data?.apply {
                if (!isActive()) {
                    throw IllegalMembershipStatusException("$policyIssuer is not active member of Business Network with $networkId ID")
                }
                if (identity.businessIdentity !is InsurerIdentity) {
                    throw IllegalMembershipBusinessIdentityException("$policyIssuer business identity should be InsurerIdentity")
                }
                if (roles.find { IssuePermissions.CAN_ISSUE_POLICY in it.permissions } == null) {
                    throw MembershipAuthorisationException("$policyIssuer is not authorised to issue insurance Polict in Business Network with $networkId ID")
                }
            } ?: throw MembershipNotFoundException("$policyIssuer is not member of Business Network with $networkId ID")
        } catch (e: IllegalStateException) {
            throw MembershipNotFoundException("$policyIssuer is not member of Business Network with $networkId ID")
        }

        bnService.getMembership(networkId, careProvider)?.state?.data?.apply {
            if (!isActive()) {
                throw IllegalMembershipStatusException("$careProvider is not active member of Business Network with $networkId ID")
            }
            if (identity.businessIdentity !is CareProviderIdentity) {
                throw IllegalMembershipBusinessIdentityException("$careProvider business identity should be CareProviderIdentity")
            }
        } ?: throw MembershipNotFoundException("$careProvider is not member of Business Network with $networkId ID")
    }
}

/**
 * Exception thrown when membership's business identity is in illegal state.
 */
class IllegalMembershipBusinessIdentityException(message: String) : FlowException(message)