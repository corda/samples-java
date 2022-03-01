package net.corda.samples.lending.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class ProjectContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.lending.contracts.ProjectContract";


    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Validation Logic Goes Here
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof Commands.ProposeProject) {
            requireThat(require -> {
                /*At here, you can structure the rules for creating a project proposal
                 * this verify method makes sure that all proposed projects from the borrower company
                 * are sound, so that banks are not going to waste any time on unqualified project proposals*/
                return null;
            });
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class ProposeProject implements Commands {}
    }
}