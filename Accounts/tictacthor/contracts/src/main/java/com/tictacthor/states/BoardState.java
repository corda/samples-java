package com.tictacthor.states;

import com.tictacthor.contracts.BoardContract;
//import javafx.util.Pair;

import kotlin.Pair;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(BoardContract.class)
public class BoardState implements LinearState {


    private UniqueIdentifier playerO;
    private UniqueIdentifier playerX;
    private AnonymousParty me;
    private AnonymousParty competitor;
    private boolean isPlayerXTurn;
    private char[][] board ;
    private UniqueIdentifier linearId;
    private Status status;

    public BoardState(UniqueIdentifier playerO, UniqueIdentifier playerX, AnonymousParty me, AnonymousParty competitor) {
        //dynamic
        this.playerO = playerO;
        this.playerX = playerX;
        this.me = me;
        this.competitor = competitor;

        //fixed
        this.isPlayerXTurn = false;
        this.board = new char[][]{{'E','E','E'},{'E','E','E'},{'E','E','E'}};
        this.linearId = new UniqueIdentifier();
        this.status = Status.GAME_IN_PROGRESS;
    }

    @ConstructorForDeserialization
    public BoardState(UniqueIdentifier playerO, UniqueIdentifier playerX,
                      AnonymousParty me, AnonymousParty competitor,
                      boolean isPlayerXTurn, UniqueIdentifier linearId,
                      char[][] board, Status status) {
        this.playerO = playerO;
        this.playerX = playerX;
        this.me = me;
        this.competitor = competitor;
        this.isPlayerXTurn = isPlayerXTurn;
        this.linearId = linearId;
        this.board = board;
        this.status = status;
    }

    @NotNull @Override
    public UniqueIdentifier getLinearId() { return this.linearId; }

    @NotNull @Override
    public List<AbstractParty> getParticipants() { return Arrays.asList(me,competitor); }


    @CordaSerializable
    public enum Status {
        GAME_IN_PROGRESS, GAME_OVER
    }

    // Returns the party of the current player
    public UniqueIdentifier getCurrentPlayerParty(){
        if(isPlayerXTurn){
            return playerX;
        }else{
            return playerO;
        }
    }

    public char[][] deepCopy(){
        char[][] newboard = new char[3][3];
        for(int i=0; i<this.board.length; i++) {
            for (int j = 0; j < this.board[i].length; j++) {
                newboard[i][j] = this.board[i][j];
            }
        }
        return newboard;
    }

    public BoardState returnNewBoardAfterMove(Pair<Integer,Integer> pos, AnonymousParty me, AnonymousParty competitor){
        if((pos.getFirst() > 2) ||(pos.getSecond()> 2)){
            throw new IllegalStateException("Invalid board index.");
        }
        char[][] newborad = this.deepCopy();
        if(isPlayerXTurn){
            newborad[pos.getFirst()][pos.getSecond()] = 'X';
        }else{
            newborad[pos.getFirst()][pos.getSecond()] = 'O';
        }
        if(BoardContract.BoardUtils.isGameOver(newborad)){
            BoardState b = new BoardState(this.playerO,this.playerX,me,competitor,!this.isPlayerXTurn,this.linearId, newborad,Status.GAME_OVER);
            return b;
        }else{
            BoardState b = new BoardState(this.playerO,this.playerX,me,competitor,!this.isPlayerXTurn, this.linearId, newborad,Status.GAME_IN_PROGRESS);
            return b;
        }
    }


    //getter setter

    public UniqueIdentifier getPlayerO() {
        return playerO;
    }

    public UniqueIdentifier getPlayerX() {
        return playerX;
    }

    public AnonymousParty getMe() {
        return me;
    }

    public AnonymousParty getCompetitor() {
        return competitor;
    }

    public boolean isPlayerXTurn() {
        return isPlayerXTurn;
    }

    public char[][] getBoard() {
        return board;
    }

    public Status getStatus() {
        return status;
    }

    public void setPlayerO(UniqueIdentifier playerO) {
        this.playerO = playerO;
    }

    public void setPlayerX(UniqueIdentifier playerX) {
        this.playerX = playerX;
    }

    public void setMe(AnonymousParty me) {
        this.me = me;
    }

    public void setCompetitor(AnonymousParty competitor) {
        this.competitor = competitor;
    }

    public void setPlayerXTurn(boolean playerXTurn) {
        isPlayerXTurn = playerXTurn;
    }

    public void setBoard(char[][] board) {
        this.board = board;
    }

    public void setLinearId(UniqueIdentifier linearId) {
        this.linearId = linearId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}