package com.tutorial.states

import com.tutorial.contracts.BasketOfApplesContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(BasketOfApplesContract::class)
class BasketOfApples(var description : String, //Brand or type
                    var farm : Party, //Origin of the apple
                    var owner: Party, //The person who exchange the basket of apple with the stamp.
                    var weight: Int)
    : ContractState {

    //Secondary Constructor
    constructor(description: String, farm: Party, weight: Int) : this(
            description = description,
            farm = farm,
            owner = farm,
            weight = weight
    )

    override var participants: List<AbstractParty> = listOf<AbstractParty>(farm,owner)

    fun changeOwner(buyer: Party): BasketOfApples {
        return BasketOfApples(description, farm, buyer, weight)
    }
}