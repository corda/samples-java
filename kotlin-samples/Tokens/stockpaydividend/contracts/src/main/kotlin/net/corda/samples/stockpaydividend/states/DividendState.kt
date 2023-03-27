package net.corda.samples.stockpaydividend.states

import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.samples.stockpaydividend.contracts.DividendContract
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(DividendContract::class)
data class DividendState(val company: Party,
                         val shareholder: Party,
                         val payDate: Date,
                         val dividendAmount: Amount<TokenType>,
                         val paid: Boolean,
                         override val linearId: UniqueIdentifier,
                         override val participants: List<AbstractParty> = listOf(company,shareholder)) : LinearState
