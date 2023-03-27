package net.corda.samples.lending.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.lending.states.LoanBidState;
import net.corda.samples.lending.states.SyndicateState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class SyndicateContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.lending.contracts.SyndicateContract";


    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Validation Logic Goes Here
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof Commands.Create) {
            requireThat(require -> {
                /*Here writes the rules for the lead bank's creating the syndication.*/

                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}
