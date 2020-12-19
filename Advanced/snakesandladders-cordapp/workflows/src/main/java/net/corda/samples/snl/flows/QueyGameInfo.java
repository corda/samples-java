package net.corda.samples.snl.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.sample.snl.states.GameBoard;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@InitiatingFlow
@StartableByRPC
public class QueyGameInfo extends FlowLogic<GameInfo> {

    private String gameId;

    public QueyGameInfo(String gameId) {
        this.gameId = gameId;
    }

    @Override
    @Suspendable
    public GameInfo call() throws FlowException {

        QueryCriteria.LinearStateQueryCriteria linearStateQueryCriteria =
                new QueryCriteria.LinearStateQueryCriteria(null,
                        Collections.singletonList(UUID.fromString(gameId)),
                        null, Vault.StateStatus.UNCONSUMED, null);
        List<StateAndRef<GameBoard>> gameBoardList =  getServiceHub().getVaultService()
                .queryBy(GameBoard.class, linearStateQueryCriteria).getStates();
        if(gameBoardList.size() ==0)
            throw new FlowException("Game doesn't exist!");

        GameBoard gameBoard = gameBoardList.get(0).getState().getData();
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        String player1 = accountService.accountInfo(gameBoard.getPlayer1().getOwningKey()).getState().getData().getName();
        String player2 = accountService.accountInfo(gameBoard.getPlayer2().getOwningKey()).getState().getData().getName();

        GameInfo gameInfo = new GameInfo(
                gameBoard.getLinearId(),
                player1,
                player2,
                gameBoard.getCurrentPlayer(),
                gameBoard.getPlayer1Pos(),
                gameBoard.getPlayer2Pos(),
                gameBoard.getWinner(),
                gameBoard.getLastRoll()
        );
        return gameInfo;
    }
}
