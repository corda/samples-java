package com.tutorial.states

import com.tutorial.contracts.AppleStampContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.ConstructorForDeserialization

@BelongsToContract(AppleStampContract::class)
class AppleStamp @ConstructorForDeserialization constructor(
    val stampDesc: String,//For example: "One stamp can exchange for a basket of HoneyCrispy Apple"
    val issuer: Party, //The person who issued the stamp
    val holder: Party, //The person who currently owns the stamp
    override val linearId: UniqueIdentifier,//LinearState required variable.
    override val participants: List<AbstractParty> = listOf<AbstractParty>(issuer, holder)
) : LinearState