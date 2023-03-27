package net.corda.samples.lending.states

import net.corda.samples.lending.contracts.SyndicateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(SyndicateContract::class)
class SyndicateState(
        override val linearId: UniqueIdentifier,
        val leadBank: Party,
        val participantBanks: List<Party>,
        val projectDetails: LinearPointer<ProjectState>,
        val loanDetails: LinearPointer<LoanBidState>,
        override val participants: List<AbstractParty> = participantBanks + listOf(leadBank)
) : LinearState
