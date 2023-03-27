package net.corda.samples.notarychange.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.contracts.CommandWithParties;
import net.corda.samples.notarychange.states.IOUState;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IOUContract implements Contract {
    public static final String ID = "net.corda.samples.notarychange.contracts.IOUContract";

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        requireThat(require -> {
            final CommandWithParties<CommandData> command = tx.getCommands().get(0);
            if( command.getValue() instanceof  Commands.Create) {
                final IOUState out = tx.outputsOfType(IOUState.class).get(0);
                // IOU-specific constraints.
                require.using("The IOU's value must be non-negative.", out.getValue() > 0);
            }
            return null;
        });
    }

    public interface Commands extends CommandData {
        class Create implements Commands { }
        class Settle implements Commands { }
    }
}
