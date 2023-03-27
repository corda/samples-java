package net.corda.samples.avatar.states

import net.corda.samples.avatar.contracts.AvatarContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import java.util.*


//Avatar can be thought of as any metaverse avatar which needs to be created and sold on at an exchange. This entity
//has an id and owner associated with it. We will see how this avatar can only be sold within a certain time limit.
@BelongsToContract(AvatarContract::class)
class Avatar(val owner: AbstractParty,
             val avatarId: String,
             override val participants: List<AbstractParty> = listOf(owner)) : ContractState {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val avatar = other as Avatar
        return avatarId == avatar.avatarId
    }

    override fun hashCode(): Int {
        return Objects.hash(avatarId)
    }
}
