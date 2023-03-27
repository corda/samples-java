package net.corda.samples.duediligence.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.samples.duediligence.contracts.CorporateRecordsContract

/*This can include reviewing incorporation documents, company constitutions,
organisational charts, a list of security holders, employee share plans and any
options granted to acquire securities.*/
@BelongsToContract(CorporateRecordsContract::class)
class CorporateRecordsAuditRequest(
        var qualification: Boolean = false,
        val applicant: Party,
        val validater: Party,
        val numberOfFiles: Int = 0,
        override val participants: List<AbstractParty> = listOf(applicant,validater),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    fun validatedAndApproved() {
        qualification = true
    }
}