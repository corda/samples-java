package net.corda.samples.observable.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.observable.states.HighlyRegulatedState;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class HighlyRegulatedContract implements Contract {
    public static final String ID = "net.corda.samples.observable.contracts.HighlyRegulatedContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties<Commands.Trade> command = requireSingleCommand(tx.getCommands(), Commands.Trade.class);
        requireThat(require -> {

            final HighlyRegulatedState out = tx.outputsOfType(HighlyRegulatedState.class).get(0);
            require.using("The Buyer and the seller cannot be the same entity.",
                    !out.getBuyer().equals(out.getSeller()));
            return null;
        });
    }

    public interface Commands extends CommandData {
        class Trade implements Commands {}
    }
}
