package net.corda.samples.notarychange.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.samples.notarychange.contracts.IOUContract
import java.util.*


/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 */
@BelongsToContract(IOUContract::class)
class IOUState
/**
 * @param value the value of the IOU.
 * @param lender the party issuing the IOU.
 * @param borrower the party receiving and approving the IOU.
 */(val value: Int,
    val lender: Party,
    val borrower: Party,
    override val linearId: UniqueIdentifier) : LinearState {
    override val participants: List<AbstractParty>
        get() = Arrays.asList(lender, borrower)

    override fun toString(): String {
        return String.format("IOUState(value=%s, lender=%s, borrower=%s, linearId=%s)", value, lender, borrower, linearId)
    }
}