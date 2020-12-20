package net.corda.sample.snl.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.sample.snl.states.BoardConfig;
import org.jetbrains.annotations.NotNull;

public class BoardConfigContract implements Contract {
    public static String ID = "net.corda.sample.snl.contracts.BoardConfigContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract verification logic should be implemented here

        if(tx.getOutputStates().size() != 1 || tx.getInputStates().size() != 0)
            throw new IllegalArgumentException("Zero Input and One Output Expected");

        if(!(tx.getOutput(0) instanceof BoardConfig))
            throw new IllegalArgumentException("Output of type BoardConfig expected");

        BoardConfig boardConfig = (BoardConfig) tx.getOutput(0);
        if(boardConfig.getSnakePositions() == null || boardConfig.getSnakePositions().size() ==0
                || boardConfig.getLadderPositions() == null || boardConfig.getLadderPositions().size() == 0)
            throw new IllegalArgumentException("Snake and Ladder Positions should not be empty or null");
    }

    public interface Commands extends CommandData {
        class Create implements GameBoardContract.Commands {}
    }
}
