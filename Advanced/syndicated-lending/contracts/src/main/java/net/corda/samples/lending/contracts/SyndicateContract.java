package net.corda.samples.lending.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.lending.states.LoanBidState;
import net.corda.samples.lending.states.SyndicateState;
import org.jetbrains.annotations.NotNull;

public class SyndicateContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Validation Logic Goes Here

        SyndicateState syndicateState = (SyndicateState) tx.getOutput(0);
        StateAndRef<LoanBidState> loanBidState = syndicateState.getLoanDetails().resolve(tx);

        if(!(loanBidState.getState().getData().getLender().equals(syndicateState.getLeadBank())))
            throw new IllegalArgumentException("Lead Bank not authorised to do this transaction");

        if(!(loanBidState.getState().getData().getStatus().equals("APPROVED")))
            throw new IllegalArgumentException("Lending Terms not approved by burrower");

    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}
