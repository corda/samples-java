package net.corda.samples.snl.flows;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class GameInfo {
    private UniqueIdentifier linearId;
    private String player1;
    private String player2;
    private String currentPlayer;
    private int player1Pos;
    private int player2Pos;
    private String winner;
    private int lastRoll;

    public GameInfo(UniqueIdentifier linearId, String player1, String player2, String currentPlayer, int player1Pos,
                    int player2Pos, String winner, int lastRoll) {
        this.linearId = linearId;
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = currentPlayer;
        this.player1Pos = player1Pos;
        this.player2Pos = player2Pos;
        this.winner = winner;
        this.lastRoll = lastRoll;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public String getPlayer1() {
        return player1;
    }

    public String getPlayer2() {
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
}
