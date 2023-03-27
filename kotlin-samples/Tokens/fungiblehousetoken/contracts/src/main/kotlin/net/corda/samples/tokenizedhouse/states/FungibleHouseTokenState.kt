package net.corda.samples.tokenizedhouse.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.samples.tokenizedhouse.contracts.HouseTokenStateContract

// *********
// * State *
// *********
@BelongsToContract(HouseTokenStateContract::class)
data class FungibleHouseTokenState(val valuation: Int,
                                   val maintainer: Party,
                                   override val linearId: UniqueIdentifier,
                                   override val fractionDigits: Int,
                                   val symbol:String,
                                   override val maintainers: List<Party> = listOf(maintainer)
                                   ) : EvolvableTokenType()