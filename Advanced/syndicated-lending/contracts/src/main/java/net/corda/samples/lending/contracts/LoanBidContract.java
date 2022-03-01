package net.corda.samples.lending.contracts;


import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class LoanBidContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.lending.contracts.LoanBidContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Validation Logic Goes Here
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof Commands.Submit) {
            requireThat(require -> {
                /* At here, the loan bid proposal from the competing banks is verified.
                These contract rules make sure that each loan bid for project is valid. */
                return null;
            });
        }else if (commandData instanceof Commands.Approve) {
            requireThat(require -> {
                /* At here, the loan bid is verified for approval process. These contract rules make
                sure that all the conditions are met for the borrower to approve the sole loan bid for its
                project. */
                return null;
            });
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Submit implements ProjectContract.Commands {}
        class Approve implements ProjectContract.Commands {}
    }
}
