package net.corda.samples.businessmembership.states

import net.corda.bn.states.BNIdentity
import net.corda.bn.states.BNPermission
import net.corda.bn.states.BNRole
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.samples.businessmembership.contracts.InsuranceStateContract
import java.util.regex.Pattern

@BelongsToContract(InsuranceStateContract::class)
data class InsuranceState(
        val insurer: Party,
        val insuree: String,
        val careProvider: Party,
        val networkId: String,
        val policyStatus: String,
        override val participants: List<AbstractParty> = listOf(insurer, careProvider),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
):LinearState {
}

/**
 * Custom Identity #1
 * Business identity specific for Insurer Companies. Uses mimicking Swift Business Identifier Code (BIC).
 *
 * @property bic Business Identifier Code of the bank.
 */
@CordaSerializable
data class InsurerIdentity(val insuranceIdentityCode: String) : BNIdentity {
    companion object {
        private const val iicRegex = "^[a-zA-Z]{6}[0-9a-zA-Z]{2}([0-9a-zA-Z]{3})?$"
    }

    /** Checks whether provided BIC is valid. **/
    fun isValid() = insuranceIdentityCode.matches(Pattern.compile(iicRegex).toRegex())
}

/**
 * Represents Policy Issuer role which has permission to issue Policy.
 */
@CordaSerializable
class PolicyIssuerRole : BNRole("PolicyIssuer", setOf(IssuePermissions.CAN_ISSUE_POLICY))

/**
 * PolicyIssuer related permissions which can be given to a role.
 */
@CordaSerializable
enum class IssuePermissions : BNPermission {

    /** Enables Business Network member to issue [InsuranceClaim]s. **/
    CAN_ISSUE_POLICY
}


/**
 * Custom Identity #2
 * Business identity specific for CareProvider Companies. Uses mimicking Swift Business Identifier Code (BIC).
 *
 * @property bic Business Identifier Code of the bank.
 */
@CordaSerializable
data class CareProviderIdentity(val cic: String) : BNIdentity {
    companion object {
        private const val cicRegex = "^[a-zA-Z]{6}[0-9a-zA-Z]{2}([0-9a-zA-Z]{3})?$"
    }

    /** Checks whether provided BIC is valid. **/
    fun isValid() = cic.matches(Pattern.compile(cicRegex).toRegex())
}

@CordaSerializable
class PsychiatryRole : BNRole("Psychiatrists", setOf(PsychiatryPermissions.CAN_TREAT_MENTAL_HEALTH_ISSUE))

@CordaSerializable
class OrthodonticsRole : BNRole("Orthodontics", setOf(OrthodonticsPermissions.CAN_TREAT_ORAL_ISSUE))


@CordaSerializable
enum class PsychiatryPermissions : BNPermission {
    /** Enables Business Network member to issue [InsuranceClaim]s. **/
    CAN_TREAT_MENTAL_HEALTH_ISSUE
}

@CordaSerializable
enum class OrthodonticsPermissions : BNPermission {
    /** Enables Business Network member to issue [InsuranceClaim]s. **/
    CAN_TREAT_ORAL_ISSUE
}