package com.tictacthor.contracts;

import com.tictacthor.states.BoardState;
import kotlin.Pair;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;

// ************
// * Contract *
// ************
public class BoardContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.tictacthor.contracts.BoardContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        /* We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a signle transaction.*/
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        //In our hello-world app, We will only have one command.
        class StartGame implements Commands {}
        class SubmitTurn implements Commands {}
        class EndGame implements Commands {}

    }


    public static class BoardUtils{
        public static Boolean isGameOver(char[][] board){
            return (board[0][0] == board [0][1] && board[0][0] == board [0][2] && (board[0][0] == 'X' || board[0][0] == 'O')) ||
                    (board[0][0] == board [1][1] && board[0][0] == board [2][2]&& (board[0][0] == 'X' || board[0][0] == 'O')) ||
                    (board[0][0] == board [1][0] && board[0][0] == board [2][0]&& (board[0][0] == 'X' || board[0][0] == 'O')) ||
                    (board[2][0] == board [2][1] && board[2][0] == board [2][2]&& (board[2][0] == 'X' || board[2][0] == 'O')) ||
                    (board[2][0] == board [1][1] && board[0][0] == board [0][2]&& (board[2][0] == 'X' || board[2][0] == 'O')) ||
                    (board[0][2] == board [1][2] && board[0][2] == board [2][2]&& (board[0][2] == 'X' || board[0][2] == 'O')) ||
                    (board[0][1] == board [1][1] && board[0][1] == board [2][1]&& (board[0][1] == 'X' || board[0][1] == 'O')) ||
                    (board[1][0] == board [1][1] && board[1][0] == board [1][2]&& (board[1][0] == 'X' || board[1][0] == 'O'));
        }

    }
}
