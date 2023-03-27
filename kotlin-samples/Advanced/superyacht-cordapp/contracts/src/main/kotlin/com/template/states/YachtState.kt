package com.template.states

import com.template.contracts.YachtContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import java.time.LocalDate
import java.util.*


// *********
// * State *
// *********
@BelongsToContract(YachtContract::class)

data class YachtState(
    val issuer: AbstractParty,
    override val owner: AbstractParty,
    val name: String,
    val type: String,
    val length: Double,
    val builderName: String,
    val yearOfBuild: Int,
    val price: Amount<Currency>,
    val forSale: Boolean,
    override val linearId: UniqueIdentifier,
    override val participants: List<AbstractParty> = listOf(owner)
) : OwnableState, LinearState {
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(
            YachtContract.Commands.Purchase(),
            YachtState(
                this.issuer,
                newOwner,
                this.name,
                this.type,
                this.length,
                this.builderName,
                this.yearOfBuild,
                this.price,
                this.forSale,
                this.linearId,
                listOf(newOwner)
            )
        )
    }
}

