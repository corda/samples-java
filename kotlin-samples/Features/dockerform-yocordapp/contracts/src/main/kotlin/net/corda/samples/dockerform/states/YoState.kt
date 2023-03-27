package net.corda.samples.dockerform.states


import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.ConstructorForDeserialization
import net.corda.samples.dockerform.contracts.YoContract
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(YoContract::class)
class YoState : ContractState {
    val origin: Party
    val target: Party
    val yo: String

    @ConstructorForDeserialization
    constructor(origin: Party, target: Party, yo: String) {
        this.origin = origin
        this.target = target
        this.yo = yo
    }

    constructor(origin: Party, target: Party) {
        this.origin = origin
        this.target = target
        yo = "Yo!"
    }

    override val participants: List<AbstractParty>
        get() = Arrays.asList(target)

    override fun toString(): String {
        return origin.name.toString() + ": " + yo
    }
}
