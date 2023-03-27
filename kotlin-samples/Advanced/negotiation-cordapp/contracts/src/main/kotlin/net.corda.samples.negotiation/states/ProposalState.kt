package net.corda.samples.negotiation.states

import net.corda.samples.negotiation.contracts.ProposalAndTradeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(ProposalAndTradeContract::class)
data class ProposalState(
        val amount: Int,
        val buyer: Party,
        val seller: Party,
        val proposer: Party,
        val proposee: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    override val participants = listOf(proposer, proposee)
}