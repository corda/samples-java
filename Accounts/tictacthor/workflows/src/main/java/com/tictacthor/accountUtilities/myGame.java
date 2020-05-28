package com.tictacthor.accountUtilities;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.AccountInfoByName;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import com.tictacthor.states.BoardState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.identity.Party;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.*;
import java.util.stream.Collectors;

@StartableByRPC
@StartableByService
public class myGame extends FlowLogic<BoardState>{

    private String whoAmI;
    public myGame(String whoAmI) {
        this.whoAmI = whoAmI;
    }

    @Override
    @Suspendable
    public BoardState call() throws FlowException {
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(whoAmI).get(0).getState().getData();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(myAccount.getIdentifier().getId()));
        BoardState b = getServiceHub().getVaultService().queryBy(BoardState.class,criteria).getStates().get(0).getState().getData();
        return b;
    }
}