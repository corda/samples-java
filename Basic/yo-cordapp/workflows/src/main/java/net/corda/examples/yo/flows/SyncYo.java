package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts;
import net.corda.examples.yo.states.YoState;

import java.util.*;

@StartableByRPC
@StartableByService
public class SyncYo extends FlowLogic<String>{
    private String whoAmI;
    private Party party;

    public SyncYo(String whoAmI, Party party) {
        this.whoAmI = whoAmI;
        this.party = party;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        //Query the yoState.
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(whoAmI).get(0).getState().getData();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(myAccount.getIdentifier().getId()));
        List<StateAndRef<YoState>> yoList = getServiceHub().getVaultService().queryBy(YoState.class,criteria).getStates();

        try {
            subFlow(new ShareStateAndSyncAccounts(yoList.get(yoList.size()-1),party));

        }catch (Exception e){
            throw new FlowException("YoState in "+whoAmI+" account not found.");
        }
        return "Yo synced";
    }
}