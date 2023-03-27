package net.corda.samples.lending.states

import net.corda.samples.lending.contracts.SyndicateBidContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(SyndicateBidContract::class)
class SyndicateBidState(
        override val linearId: UniqueIdentifier,
        val syndicateState: LinearPointer<SyndicateState>,
        val bidAmount: Int,
        val leadBank: Party,
        val participateBank: Party,
        val status: String,
        override val participants: List<AbstractParty> = listOf(participateBank, leadBank)) : LinearState
