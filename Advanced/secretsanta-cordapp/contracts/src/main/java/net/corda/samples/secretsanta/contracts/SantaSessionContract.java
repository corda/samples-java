package net.corda.samples.secretsanta.contracts;

import net.corda.samples.secretsanta.states.SantaSessionState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class SantaSessionContract implements Contract {

    public static final String ID = "net.corda.samples.secretsanta.contracts.SantaSessionContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        /*
         * We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a signle transaction.
         */
        final CommandWithParties<SantaSessionContract.Commands> command = requireSingleCommand(tx.getCommands(), SantaSessionContract.Commands.class);

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if (command.getValue() instanceof SantaSessionContract.Commands.Issue) {

            // Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                // no inputs to the transaction
                require.using("No inputs should be consumed when creating a new SantaSession.", inputs.isEmpty());

                require.using("Transaction must have no input.", inputs.size() == 0);
                require.using("Transaction must have exactly one output.", outputs.size() == 1);

                require.using("Output must be a SantaSessionState.", outputs.get(0) instanceof SantaSessionState);

                // Retrieve the output state of the transaction
                SantaSessionState output = (SantaSessionState) outputs.get(0);

                // must be three or more players
                require.using("Must be three or more players", output.getPlayerNames().size() > 2);

                require.using("Contact info for each player", output.getPlayerEmails().size() == output.getPlayerNames().size());

                // each player has only one person assigned
                //require.using("Each Player should have one person assigned", output.getAssignments().size() == output.getPlayerNames().size());

                return null;
            });

        } else {
            throw new IllegalArgumentException("Command not found!");
        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        // In our secret santa app we only issue new Secret Santa game sessions shared with all players accounts
        class Issue implements Commands {}
    }
}
