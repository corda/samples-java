package net.corda.samples.lending.contracts;


import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class LoanBidContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
            // Contract Validation Logic Goes Here
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Submit implements ProjectContract.Commands {}
        class Approve implements ProjectContract.Commands {}
    }
}
