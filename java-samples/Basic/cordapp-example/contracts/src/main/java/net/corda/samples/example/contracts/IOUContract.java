package net.corda.samples.example.contracts;

import net.corda.samples.example.states.IOUState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * An implementation of a basic smart contract in Corda.
 *
 * This contracts enforces rules regarding the creation of a valid [IOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output states: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must subclass the [Contract] interface.
 */
public class IOUContract implements Contract {
    public static final String ID = "net.corda.samples.example.contracts.IOUContract";

    /**
     * The "verify()" function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an IOU.",
                    tx.getInputs().isEmpty());
            require.using("Only one output states should be created.",
                    tx.getOutputs().size() == 1);
            final IOUState out = tx.outputsOfType(IOUState.class).get(0);
            require.using("The lender and the borrower cannot be the same entity.",
                    !out.getLender().equals(out.getBorrower()));
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            // IOU-specific constraints.
            require.using("The IOU's value must be non-negative.",
                    out.getValue() > 0);

            return null;
        });
    }

    /**
     * This contracts only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}