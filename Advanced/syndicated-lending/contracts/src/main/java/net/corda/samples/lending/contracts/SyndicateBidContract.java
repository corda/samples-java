package net.corda.samples.lending.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class SyndicateBidContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Validation Logic Goes Here
    }

    public interface Commands extends CommandData {
        class Submit implements Commands {}
        class Approve implements Commands {}
    }
}
