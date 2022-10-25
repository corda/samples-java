package net.corda.samples.multioutput.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.multioutput.states.OmniCountState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class OmniCountContract implements Contract {
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);
        if (command.getValue() instanceof Commands.dualUpdate) {
            requireThat(require -> {
                OmniCountState outputState = tx.outputsOfType(OmniCountState.class).get(0);
                require.using("The Omni Balance cannot exceed 200", outputState.getOmniAmount() < 200);
                return null;
            });
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        //In our hello-world app, We will only have one command.
        class Send implements OmniCountContract.Commands {}
        class dualUpdate implements OmniCountContract.Commands {}

    }
}
