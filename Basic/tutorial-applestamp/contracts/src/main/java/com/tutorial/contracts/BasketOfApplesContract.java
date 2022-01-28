package com.tutorial.contracts;

import com.tutorial.states.AppleStamp;
import com.tutorial.states.BasketOfApples;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class BasketOfApplesContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.tutorial.contracts.BasketOfApplesContract";


    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        //Extract the command from the transaction.
        final CommandData commandData = tx.getCommands().get(0).getValue();

        if (commandData instanceof BasketOfApplesContract.Commands.packBasket){
            BasketOfApples output = tx.outputsOfType(BasketOfApples.class).get(0);
            requireThat(require -> {
                require.using("This transaction should only output one BasketOfApples state", tx.getOutputs().size() == 1);
                require.using("The output BasketOfApples state should have clear description of Apple product", !output.getDescription().equals(""));
                require.using("The output BasketOfApples state should have non zero weight", output.getWeight() > 0);
                return null;
            });
        }
        else if (commandData instanceof BasketOfApplesContract.Commands.Redeem) {
            //Retrieve the output state of the transaction
            AppleStamp input = tx.inputsOfType(AppleStamp.class).get(0);
            BasketOfApples output = tx.outputsOfType(BasketOfApples.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("This transaction should consume two states", tx.getInputStates().size() == 2);
                require.using("The issuer of the Apple stamp should be the producing farm of this basket of apple", input.getIssuer().equals(output.getFarm()));
                require.using("The basket of apple has to weight more than 0", output.getWeight() > 0);
                return null;
            });
        }
        else{
            //Unrecognized Command type
            throw new IllegalArgumentException("Incorrect type of BasketOfApples Commands");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class packBasket implements BasketOfApplesContract.Commands {}
        class Redeem implements BasketOfApplesContract.Commands {}

    }
}
