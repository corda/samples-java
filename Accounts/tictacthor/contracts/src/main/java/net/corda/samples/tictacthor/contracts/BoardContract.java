package net.corda.samples.tictacthor.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.tictacthor.states.BoardState;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class BoardContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.tictacthor.contracts.BoardContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<BoardContract.Commands> command = requireSingleCommand(tx.getCommands(), BoardContract.Commands.class);

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if (command.getValue() instanceof BoardContract.Commands.StartGame) {

            // Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("No inputs should be consumed when creating a new Invoice State.", inputs.isEmpty());
                require.using("Transaction must have exactly one output.", outputs.size() == 1);
                BoardState output = (BoardState) outputs.get(0);
                require.using("Output board must have status GAME_IN_PROGRESS", output.getStatus() == BoardState.Status.GAME_IN_PROGRESS);
                require.using("You cannot play a game with yourself.", output.getPlayerO() != output.getPlayerX());
                return null;
            });

        } else if (command.getValue() instanceof BoardContract.Commands.SubmitTurn){

        }else if (command.getValue() instanceof BoardContract.Commands.EndGame){

        }else{
            throw new IllegalArgumentException("Command not found!");
        }

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
