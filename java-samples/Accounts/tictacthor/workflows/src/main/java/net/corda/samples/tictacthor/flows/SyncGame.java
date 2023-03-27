package net.corda.samples.tictacthor.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.samples.tictacthor.states.BoardState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts;

import java.util.*;

@StartableByRPC
@StartableByService
public class SyncGame extends FlowLogic<String>{
    private String gameId;
    private Party party;

    public SyncGame(String gameId, Party party) {
        this.gameId = gameId;
        this.party = party;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        UUID id = UUID.fromString(gameId);
        QueryCriteria.LinearStateQueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(id)).withStatus(Vault.StateStatus.UNCONSUMED);
        try {
            StateAndRef<BoardState> inputBoardStateAndRef = getServiceHub().getVaultService().queryBy(BoardState.class,queryCriteria).getStates().get(0);
            subFlow(new ShareStateAndSyncAccounts(inputBoardStateAndRef,party));

        }catch (Exception e){
            throw new FlowException("GameState with id "+gameId+" not found.");
        }
        return "Game synced";
    }
}