package net.corda.samples.chainmail.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.chainmail.states.MessageState;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class MessageContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.chainmail.contracts.MessageContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        /*
         * We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a single transaction.
         */
        final CommandWithParties<MessageContract.Commands> command = requireSingleCommand(tx.getCommands(), MessageContract.Commands.class);

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if(command.getValue() instanceof MessageContract.Commands.Create) {
            // Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                MessageState output = (MessageState) outputs.get(0);
                // message must have a length
                require.using("Message must have a length", output.getMessage().length() > 0);
                return null;
            });
        } else {
            throw new IllegalArgumentException("Command not found");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}
