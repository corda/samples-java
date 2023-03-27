package net.corda.samples.lending.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class SyndicateBidContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.lending.contracts.SyndicateBidContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Validation Logic Goes Here
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof Commands.Submit) {
            requireThat(require -> {
                /* At here, the syndication bid proposal from the syndication participating banks is verified.
                These contract rules make sure that each bid for syndicated loan is valid. */
                return null;
            });
        }else if (commandData instanceof Commands.Approve) {
            requireThat(require -> {
                /* At here, the syndicated bid is verified for approval process. These contract rules make
                sure that all the conditions are met for the lead bank to approve the each syndicated bid. */
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Submit implements Commands {}
        class Approve implements Commands {}
    }
}
