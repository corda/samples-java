package net.corda.sample.snl.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.sample.snl.states.BoardConfig;
import net.corda.sample.snl.states.GameBoard;
import org.jetbrains.annotations.NotNull;

public class GameBoardContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract verification logic should be implemented here
        if(tx.getCommands().size() != 1)
            throw new IllegalArgumentException("One command Expected");

        if(tx.getCommand(0).getValue() instanceof Commands.Create){
            verifyCreate(tx);
        }else if(tx.getCommand(0).getValue() instanceof Commands.PlayMove){
            verifyPlay(tx);
        }

    }

    private void verifyCreate(LedgerTransaction tx) throws IllegalArgumentException{
        // Contract verification logic for create game should be implemented here
        if(tx.getOutputStates().size() != 1 || tx.getInputStates().size() != 0)
            throw new IllegalArgumentException("Zero Input and One Output Expected");

        if(!(tx.getOutput(0) instanceof GameBoard))
            throw new IllegalArgumentException("Output of type GameBoard expected");


    }

    private void  verifyPlay(LedgerTransaction tx) throws IllegalArgumentException {
        // Contract verification logic for play move should be implemented here

        if(tx.getReferences().size() == 0 || !(tx.getReferenceInput(0) instanceof BoardConfig)){
            throw new IllegalArgumentException("One reference Input of BoardConfig Expected");
        }

        if(tx.getOutputStates().size() != 1 || tx.getInputStates().size() != 1)
            throw new IllegalArgumentException("One Input and One Output Expected");

    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
        class PlayMove implements Commands {
            private int roll;

            public PlayMove(int roll) {
                this.roll = roll;
            }

            public int getRoll() {
                return roll;
            }
        }
    }
}
