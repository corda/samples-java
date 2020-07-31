package net.corda.examples.notarychange.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class IOUContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Verification Logic Goes Here
    }

    public interface Commands extends CommandData {
        class Create implements Commands { }
        class Settle implements Commands { }
    }
}
