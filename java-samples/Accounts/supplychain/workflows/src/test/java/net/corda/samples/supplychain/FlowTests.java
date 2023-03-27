package net.corda.samples.supplychain;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.samples.supplychain.accountUtilities.CreateNewAccount;
import net.corda.samples.supplychain.accountUtilities.ShareAccountTo;
import net.corda.samples.supplychain.flows.SendInvoice;
import net.corda.samples.supplychain.states.InvoiceState;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;


    private NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.supplychain.contracts"),
                TestCordapp.findCordapp("net.corda.samples.supplychain.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"))).withNetworkParameters(testNetworkParameters)
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
        );
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void AccountCreation() throws ExecutionException, InterruptedException {
        CreateNewAccount createAcct = new CreateNewAccount("TestAccountA");
        Future<String> future = a.startFlow(createAcct);
        network.runNetwork();
        AccountService accountService = a.getServices().cordaService(KeyManagementBackedAccountService.class);
        List<StateAndRef<AccountInfo>> myAccount = accountService.accountInfo("TestAccountA");
        assert (myAccount.size() != 0);
    }

    @Test
    public void InvoiceFlowTest() throws ExecutionException, InterruptedException {
        CreateNewAccount createAcct = new CreateNewAccount("TestAccountA");
        Future<String> future = a.startFlow(createAcct);
        network.runNetwork();
        ShareAccountTo shareAToB = new ShareAccountTo("TestAccountA",b.getInfo().getLegalIdentities().get(0));
        Future<String> future2 = a.startFlow(shareAToB);
        network.runNetwork();

        CreateNewAccount createAcct2 = new CreateNewAccount("TestAccountB");
        Future<String> future3 = b.startFlow(createAcct2);
        network.runNetwork();

        ShareAccountTo shareBToA = new ShareAccountTo("TestAccountB",a.getInfo().getLegalIdentities().get(0));
        Future<String> future4 = b.startFlow(shareBToA);
        network.runNetwork();

        SendInvoice invoiceflow = new SendInvoice("TestAccountA","TestAccountB",20);
        Future<String> future5 = a.startFlow(invoiceflow);
        network.runNetwork();

        //retrieve
        AccountService accountService = b.getServices().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo("TestAccountB").get(0).getState().getData();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria()
                .withExternalIds(Arrays.asList(myAccount.getIdentifier().getId()));
        InvoiceState storedState = b.getServices().getVaultService().queryBy(InvoiceState.class,criteria).getStates()
                .get(0).getState().getData();
        assert (storedState.getAmount() == 20);
    }


}
