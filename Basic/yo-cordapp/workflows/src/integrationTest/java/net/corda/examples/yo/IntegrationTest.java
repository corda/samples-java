package net.corda.examples.yo;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.examples.yo.accountUtilities.CreateNewAccount;
import net.corda.examples.yo.accountUtilities.ShareAccountTo;
import net.corda.examples.yo.flows.YoFlow;
import net.corda.examples.yo.states.YoState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.User;
import org.junit.Test;
import net.corda.core.concurrent.CordaFuture;
import net.corda.testing.driver.NodeHandle;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;

/**
 * Allows you to run your nodes through an IDE (as opposed to using deployNodes). Do not use in a production
 * environment.
 */
public class IntegrationTest {
    private final CordaX500Name A = new CordaX500Name("BankA", "Test1", "GB");
    private final CordaX500Name B = new CordaX500Name("BankB", "Test2", "US");

    private final TestIdentity partyA = new TestIdentity(this.A);
    private final TestIdentity partyB = new TestIdentity(this.B);
    private final List<TestCordapp> corDapps = Arrays.asList(TestCordapp.findCordapp("net.corda.examples.yo.flows"),
            TestCordapp.findCordapp("net.corda.examples.yo.contracts"),
            TestCordapp.findCordapp("net.corda.examples.yo.accountUtilities"),
            TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
            TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.ci.workflows"));
    private User RPC_USER = new User("user1", "test",  new HashSet<>(Arrays.asList("ALL")));


    @Test
    public void networkMapCall() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {
            // Start a pair of nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(partyA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(partyB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
                // and other important metrics to ensure that your CorDapp is working as intended.
                assertEquals(partyAHandle.getRpc().wellKnownPartyFromX500Name(partyB.getName()).getName(), partyB.getName());
                assertEquals(partyBHandle.getRpc().wellKnownPartyFromX500Name(partyA.getName()).getName(), partyA.getName());
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test: ", e);
            }

            return null;
        });
    }

    @Test
    public void createAndShareAccount(){
        driver(new DriverParameters()
                .withIsDebug(true)
                .withStartNodesInProcess(true)
                .withCordappsForAllNodes(this.corDapps), dsl -> {
            //Start the node
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(partyA.getName()).withRpcUsers(Arrays.asList(RPC_USER))),
                    dsl.startNode(new NodeParameters().withProvidedName(partyB.getName()).withRpcUsers(Arrays.asList(RPC_USER)))
            );
            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                //create proxies
                CordaRPCClient aClient = new CordaRPCClient(partyAHandle.getRpcAddress());
                CordaRPCOps aProxy  = aClient.start("user1", "test").getProxy();
                CordaRPCClient bClient = new CordaRPCClient(partyBHandle.getRpcAddress());
                CordaRPCOps bProxy  = bClient.start("user1", "test").getProxy();

                // Excute flows
                // Create & Share account
                aProxy.startTrackedFlowDynamic(CreateNewAccount.class,"PeterLi").getReturnValue().get();
                aProxy.startTrackedFlowDynamic(ShareAccountTo.class, "PeterLi", aProxy.wellKnownPartyFromX500Name(partyB.getName())).getReturnValue().get();
                List<StateAndRef<AccountInfo>> aAccounts = aProxy.vaultQuery(AccountInfo.class).getStates();
                List<StateAndRef<AccountInfo>> bAccounts = bProxy.vaultQuery(AccountInfo.class).getStates();

                bProxy.startTrackedFlowDynamic(CreateNewAccount.class,"DavidA").getReturnValue().get();//"BankA", "Test1", "GB"
                List<StateAndRef<AccountInfo>> bAccountsAfterShare = bProxy.vaultQuery(AccountInfo.class).getStates();
                bProxy.startTrackedFlowDynamic(ShareAccountTo.class, "DavidA", bProxy.wellKnownPartyFromX500Name(partyA.getName())).getReturnValue().get();
                List<StateAndRef<AccountInfo>> aAccountsAfterShare = aProxy.vaultQuery(AccountInfo.class).getStates();

                // ensure that both nodes have both accounts.
                assertEquals(aAccountsAfterShare.size(), 2);
                assertEquals(bAccountsAfterShare.size(), 2);

                //send yo flow
                aProxy.startTrackedFlowDynamic(YoFlow.class,"PeterLi","DavidA").getReturnValue().get();
                List<StateAndRef<YoState>> yoStates= aProxy.vaultQuery(YoState.class).getStates();

                // ensure our yo is received
                YoState yo = yoStates.get(0).getState().getData();
                assertEquals(yo.getYo(),"Yo to Accounts");

            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test: ", e);
            }

            return null;
        });
        }


}
