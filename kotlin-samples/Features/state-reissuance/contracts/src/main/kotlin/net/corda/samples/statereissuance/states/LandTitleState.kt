package net.corda.samples.statereissuance.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.samples.statereissuance.contracts.LandTitleContract

@BelongsToContract(LandTitleContract::class)
data class LandTitleState(override val linearId: UniqueIdentifier,
                          val dimensions: String,
                          val area: String,
                          val issuer: Party,
                          val owner: Party,
                          override val participants: List<AbstractParty> = listOf(issuer, owner)
) : LinearState {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o == null || javaClass != o.javaClass) false else linearId.equals((o as LandTitleState).linearId)
    }

    override fun hashCode(): Int {
        return linearId.hashCode()
    }
}