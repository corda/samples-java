package net.corda.samples.duediligence.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.samples.duediligence.contracts.DueDChecklistContract
import java.util.ArrayList

@BelongsToContract(DueDChecklistContract::class)
class DueDChecklist(
        private var numberOfapprovalsNeeded: Int = 0,
        private var status: String? = "INCOMPLETE",
        private var operationNode: Party,
        private var reportTo: Party,
        private var attachedApprovals: List<UniqueIdentifier> = listOf(),
        override val participants: List<AbstractParty> = listOf(operationNode,reportTo),
        override val linearId: UniqueIdentifier
):LinearState {

    fun uploadApproval(approvalId: UniqueIdentifier) {
        val copyOfExistingList = mutableListOf<UniqueIdentifier>()
        for (id in attachedApprovals) {
            copyOfExistingList += (id)
        }
        copyOfExistingList += (approvalId)
        attachedApprovals = copyOfExistingList
    }
}