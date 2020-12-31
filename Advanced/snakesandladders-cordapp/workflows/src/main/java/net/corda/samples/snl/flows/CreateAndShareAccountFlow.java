package net.corda.samples.snl.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;

import java.util.List;
import java.util.stream.Collectors;

@StartableByRPC
@InitiatingFlow
public class CreateAndShareAccountFlow extends FlowLogic<String> {

    private final String accountName;

    public CreateAndShareAccountFlow(String accountName) {
        this.accountName = accountName;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        //Call inbuilt CreateAccount flow to create the AccountInfo object
        StateAndRef<AccountInfo> accountInfoStateAndRef =
                (StateAndRef<AccountInfo>) subFlow(new CreateAccount(accountName));

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party oracle = getServiceHub().getNetworkMapCache()
                .getNodeByLegalName(CordaX500Name.parse("O=Oracle,L=Mumbai,C=IN")).getLegalIdentities().get(0);

        List<Party> parties = getServiceHub().getNetworkMapCache().getAllNodes().stream()
                .map(nodeInfo -> nodeInfo.getLegalIdentities().get(0))
                .collect(Collectors.toList());
        parties.remove(getOurIdentity());
        parties.remove(notary);
        parties.remove(oracle);

        //Share this AccountInfo object with the parties who want to transact with this account
        subFlow(new ShareAccountInfo(accountInfoStateAndRef, parties));
        return "" + accountName +"has been created and shared to " + parties+".";
    }
}
