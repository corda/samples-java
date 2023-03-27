package net.corda.samples.lending.states

import net.corda.samples.lending.contracts.ProjectContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(ProjectContract::class)
class ProjectState(
        override val linearId: UniqueIdentifier,
        val projectDescription: String,
        val borrower: Party,
        val projectCost: Int,
        val loanAmount: Int,
        var lenders: List<Party>,
        override val participants: List<AbstractParty>  = lenders + listOf(borrower)
) : LinearState,ContractState

