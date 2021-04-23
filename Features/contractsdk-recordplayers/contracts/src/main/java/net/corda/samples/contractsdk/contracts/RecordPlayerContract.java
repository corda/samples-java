package net.corda.samples.contractsdk.contracts;

import com.r3.corda.lib.contracts.contractsdk.StandardContract;
import com.r3.corda.lib.contracts.contractsdk.annotations.RequireNumberOfStatesOnInput;
import com.r3.corda.lib.contracts.contractsdk.annotations.RequireNumberOfStatesOnOutput;
import com.r3.corda.lib.contracts.contractsdk.annotations.RequireSignersFromEachInputState;
import com.r3.corda.lib.contracts.contractsdk.verifiers.StandardCommand;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.contractsdk.states.RecordPlayerState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


/*
You'll notice some nice things about this contract code,
the first is that you can have really small command definitions with a lot of flexibility
the second is that you now have logical grouping of your contract verification and your command definition.

This makes writing your contract code cleaner and more intuitive.
*/
public class RecordPlayerContract extends StandardContract implements Contract {
    // This id must be used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.contractsdk.contracts.RecordPlayerContract";


    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {

        // note the annotations here from the Contracts SDK
        @RequireNumberOfStatesOnInput(value = 0)
        @RequireNumberOfStatesOnOutput(value = 1)
        @RequireSignersFromEachInputState(roles = "manufacturer")
        class Issue implements Commands {
        }

        // with these annotations we can ignore worrying about many
        // aspects of the transaction we're not as interested in.
        @RequireNumberOfStatesOnInput(value = 1)
        @RequireNumberOfStatesOnOutput(value = 1)
        class Update implements Commands, StandardCommand {

            // We can add additional logic to the update command without adding extra boilerplate
            @Override
            public void verifyFurther(@NotNull LedgerTransaction tx) {

                final CommandWithParties<RecordPlayerContract.Commands> command = requireSingleCommand(tx.getCommands(), RecordPlayerContract.Commands.class);

                List<ContractState> inputs = tx.getInputStates();
                List<ContractState> outputs = tx.getOutputStates();

                RecordPlayerState oldRp = (RecordPlayerState) inputs.get(0);
                RecordPlayerState newRp = (RecordPlayerState) outputs.get(0);


                // We can still use Corda DSL function requireThat to replicate conditions-checks
                requireThat(require -> {

                    require.using("Magenetic Strength must be above 0", newRp.getMagneticStrength() > 0);
                    require.using("Magenetic Strength is too high", newRp.getMagneticStrength() < 100000);

                    require.using("Coil turns can't be negative", oldRp.getCoilTurns() > 0);
                    require.using("Coil turns can't be negative", newRp.getCoilTurns() > 0);

                    require.using("Coil turns too high", newRp.getCoilTurns() < 100000);

                    require.using("songsPlayed should never decrease", oldRp.getSongsPlayed() <= newRp.getSongsPlayed());

                    return null;
                });

            }

        }

    }
}

