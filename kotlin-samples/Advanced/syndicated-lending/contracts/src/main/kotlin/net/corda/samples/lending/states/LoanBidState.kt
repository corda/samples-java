package net.corda.samples.lending.states

import net.corda.samples.lending.contracts.LoanBidContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StaticPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(LoanBidContract::class)
class LoanBidState(
        val projectDetails: StaticPointer<ProjectState>,
        override val linearId: UniqueIdentifier,
        val lender: Party,
        val borrower: Party,
        val loanAmount: Int,
        val tenure: Int,
        val rateofInterest: Double,
        val transactionFees: Int,
        val status: String,
        override val participants: List<AbstractParty> = listOf(lender, borrower)) : LinearState