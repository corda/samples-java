package net.corda.examples.yo;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.examples.yo.accountUtilities.CreateNewAccount;
import net.corda.examples.yo.accountUtilities.ShareAccountTo;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MultiFeatureUnitTest {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters(
                        Arrays.asList(
                                TestCordapp.findCordapp("net.corda.examples.yo.flows"),
                                TestCordapp.findCordapp("net.corda.examples.yo.contracts"),
                                TestCordapp.findCordapp("net.corda.examples.yo.accountUtilities")
                                )
                )
        );
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void createAndShareAccountWithCounterParty() throws Exception {

        CreateNewAccount createNodeAAccount = new CreateNewAccount("PeterLi");
        CordaFuture<String> future = nodeA.startFlow(createNodeAAccount);
        network.runNetwork();

        CreateNewAccount createNodeBAccount = new CreateNewAccount("DavidA");
        CordaFuture<String> future2 = nodeB.startFlow(createNodeBAccount);
        network.runNetwork();

        ShareAccountTo shareAtoB = new ShareAccountTo("PeterLi",nodeB.getInfo().getLegalIdentities().get(0));
        CordaFuture<String> future3 = nodeA.startFlow(shareAtoB);
        network.runNetwork();

        ShareAccountTo shareBtoA = new ShareAccountTo("DavidA",nodeA.getInfo().getLegalIdentities().get(0));
        CordaFuture<String> future4 = nodeB.startFlow(shareBtoA);
        network.runNetwork();

        nodeA.transaction(() ->{
           try{
               List<StateAndRef<AccountInfo>> allAccountofA = nodeA.getServices().cordaService(KeyManagementBackedAccountService.class).allAccounts();
               List<StateAndRef<AccountInfo>> allAccountofB = nodeB.getServices().cordaService(KeyManagementBackedAccountService.class).allAccounts();
               assertEquals(allAccountofA, allAccountofB);
               return null;
           }catch(Exception exception){
               System.out.println(exception);

           }
            return null;
        });
    }
}


