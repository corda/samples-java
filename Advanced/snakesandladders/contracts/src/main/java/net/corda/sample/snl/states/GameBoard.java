package net.corda.sample.snl.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.sample.snl.contracts.GameBoardContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(GameBoardContract.class)
public class GameBoard implements LinearState {

    private UniqueIdentifier linearId;
    private AbstractParty player1;
    private AbstractParty player2;
    private String currentPlayer;
    private int player1Pos;
    private int player2Pos;
    private String winner;
    private int lastRoll;

    public GameBoard(UniqueIdentifier linearId, AbstractParty player1, AbstractParty player2,
                     String currentPlayer, int player1Pos, int player2Pos, String winner, int lastRoll) {
        this.linearId = linearId;
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = currentPlayer;
        this.player1Pos = player1Pos;
        this.player2Pos = player2Pos;
        this.winner = winner;
        this.lastRoll = lastRoll;
    }

    public AbstractParty getPlayer1() {
        return player1;
    }

    public AbstractParty getPlayer2() {
        return player2;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public int getPlayer1Pos() {
        return player1Pos;
    }

    public int getPlayer2Pos() {
        return player2Pos;
    }

    public String getWinner() {
        return winner;
    }

    public int getLastRoll() {
        return lastRoll;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(player1, player2);
    }
}
