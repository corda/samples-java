package net.corda.samples.snl.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import com.sun.istack.NotNull;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ReferencedStateAndRef;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.sample.snl.contracts.GameBoardContract;
import net.corda.sample.snl.states.BoardConfig;
import net.corda.sample.snl.states.GameBoard;
import net.corda.samples.snl.oracle.flows.OracleSignatureFlow;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerMoveFlow {

    private PlayerMoveFlow() {}

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private String player;
        private String linearId;
        private int diceRolled;

        public Initiator(String player, String linearId, int diceRolled) {
            this.player = player;
            this.linearId = linearId;
            this.diceRolled = diceRolled;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);

            //Get current player account info
            List<StateAndRef<AccountInfo>> currentPlayerAccountInfo = accountService.accountInfo(player);
            if(currentPlayerAccountInfo.size() ==0)
                throw new FlowException("Player "+ player + " doesn't exist!");

            AbstractParty currPlayer = subFlow(new RequestKeyForAccount(currentPlayerAccountInfo.get(0).getState().getData()));

            // Get game board
            QueryCriteria.LinearStateQueryCriteria linearStateQueryCriteria =
                    new QueryCriteria.LinearStateQueryCriteria(null,
                            Collections.singletonList(UUID.fromString(linearId)),
                            null, Vault.StateStatus.UNCONSUMED, null);
            List<StateAndRef<GameBoard>> gameBoardList =  getServiceHub().getVaultService()
                    .queryBy(GameBoard.class, linearStateQueryCriteria).getStates();
            if(gameBoardList.size() ==0)
                throw new FlowException("Game doesn't exist!");

            GameBoard gameBoard = gameBoardList.get(0).getState().getData();

            if(gameBoard.getWinner() !=null){
                throw new FlowException("This Game is Over");
            }

            // Check if the initiator is the current player
            if(!gameBoard.getCurrentPlayer().equals(player))
                throw new FlowException("Please wait for your turn");

            // Get the other player
            AtomicReference<AccountInfo> otherPlayerAccountInfo = new AtomicReference<>();
            gameBoard.getParticipants().forEach(abstractParty -> {
                AccountInfo thisAI = accountService.accountInfo(abstractParty.getOwningKey()).getState().getData();
                if(!thisAI.getName().equals(player)){
                    otherPlayerAccountInfo.set(accountService.accountInfo(abstractParty.getOwningKey()).getState().getData());
                }
            });
            AbstractParty otherPlayer = subFlow(new RequestKeyForAccount(otherPlayerAccountInfo.get()));

            List<StateAndRef<BoardConfig>> boardConfigList =
                    getServiceHub().getVaultService().queryBy(BoardConfig.class).getStates();
            if(boardConfigList.size() == 0)
                throw new FlowException("Board config missing");

            BoardConfig boardConfig = boardConfigList.get(0).getState().getData();

            // Calculate Player Position
            int newPlayer1Pos = gameBoard.getPlayer1Pos();
            int newPlayer2Pos = gameBoard.getPlayer2Pos();
            String winner = null;
            if(player.equals(accountService.accountInfo(gameBoard.getPlayer1().getOwningKey()).getState().getData().getName())){
                newPlayer1Pos = gameBoard.getPlayer1Pos() + diceRolled;
                if(newPlayer1Pos > 100){
                    throw new FlowException("You need to roll " + (100 - gameBoard.getPlayer1Pos()) + " to win.");
                }
                if(boardConfig.getLadderPositions().keySet().contains(newPlayer1Pos))
                    newPlayer1Pos = boardConfig.getLadderPositions().get(newPlayer1Pos);
                else if(boardConfig.getSnakePositions().keySet().contains(newPlayer1Pos))
                    newPlayer1Pos = boardConfig.getSnakePositions().get(newPlayer1Pos);

                if(newPlayer1Pos == 100){
                    winner = accountService.accountInfo(gameBoard.getPlayer1().getOwningKey()).getState().getData().getName();
                }

            }else{
                newPlayer2Pos = gameBoard.getPlayer2Pos() + diceRolled;
                if(newPlayer2Pos > 100){
                    throw new FlowException("You need to roll " + (100 - gameBoard.getPlayer2Pos()) + " to win.");
                }
                if(boardConfig.getLadderPositions().keySet().contains(newPlayer2Pos))
                    newPlayer2Pos = boardConfig.getLadderPositions().get(newPlayer2Pos);
                else if(boardConfig.getSnakePositions().keySet().contains(newPlayer2Pos))
                    newPlayer2Pos = boardConfig.getSnakePositions().get(newPlayer2Pos);

                if(newPlayer2Pos == 100){
                    winner = accountService.accountInfo(gameBoard.getPlayer2().getOwningKey()).getState().getData().getName();
                }
            }

            GameBoard outputGameBoard = new GameBoard(gameBoard.getLinearId(),
                    gameBoard.getPlayer1(), gameBoard.getPlayer2(), otherPlayerAccountInfo.get().getName(),
                    newPlayer1Pos, newPlayer2Pos, winner, diceRolled);

            TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                    .addInputState(gameBoardList.get(0))
                    .addOutputState(outputGameBoard)
                    .addCommand(new GameBoardContract.Commands.PlayMove(diceRolled), Arrays.asList(currPlayer.getOwningKey(),
                            otherPlayer.getOwningKey()))
                    .addReferenceState(new ReferencedStateAndRef<>(boardConfigList.get(0)));

            transactionBuilder.verify(getServiceHub());

            SignedTransaction selfSignedTransaction =
                    getServiceHub().signInitialTransaction(transactionBuilder, currPlayer.getOwningKey());

            Party oracle = getServiceHub().getNetworkMapCache()
                    .getNodeByLegalName(CordaX500Name.parse("O=Oracle,L=Mumbai,C=IN")).getLegalIdentities().get(0);

            FilteredTransaction ftx = selfSignedTransaction.buildFilteredTransaction(o -> {
                if (o instanceof Command && ((Command) o).getSigners().contains(oracle.getOwningKey())
                        && ((Command) o).getValue() instanceof GameBoardContract.Commands.PlayMove) {
                    return  true;
                }
                return false;
            });

            TransactionSignature oracleSignature = subFlow(new OracleSignatureFlow(oracle, ftx));
            SignedTransaction selfAndOracleSignedTransaction = selfSignedTransaction.withAdditionalSignature(oracleSignature);


            FlowSession otherPlayerSession =  initiateFlow(otherPlayerAccountInfo.get().getHost());
            SignedTransaction signedTransaction =
                    subFlow(new CollectSignaturesFlow(selfAndOracleSignedTransaction, Collections.singletonList(otherPlayerSession),
                            Collections.singletonList(currPlayer.getOwningKey())));

            if(!otherPlayerAccountInfo.get().getHost().equals(getOurIdentity())) {
                signedTransaction = subFlow(new FinalityFlow(signedTransaction, Collections.singletonList(otherPlayerSession)));
                try {
                    accountService.shareStateAndSyncAccounts(signedTransaction
                            .toLedgerTransaction(getServiceHub()).outRef(0) ,otherPlayerSession.getCounterparty());
                } catch (SignatureException e) {
                    e.printStackTrace();
                }
            }
            else{
                signedTransaction = subFlow(new FinalityFlow(signedTransaction, Collections.emptyList()));
            }
            return signedTransaction;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{
        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Custom Logic to validate transaction.
                }
            });
            if(!counterpartySession.getCounterparty().equals(getOurIdentity()))
                return  subFlow(new ReceiveFinalityFlow(counterpartySession));

            return null;
        }
    }
}
