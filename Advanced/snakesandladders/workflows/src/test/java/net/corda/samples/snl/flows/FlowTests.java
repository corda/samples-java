package net.corda.samples.snl.flows;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.sample.snl.states.BoardConfig;
import net.corda.samples.snl.flows.CreateAndShareAccountFlow;
import net.corda.samples.snl.flows.CreateBoardConfig;
import net.corda.samples.snl.flows.CreateGameFlow;
import net.corda.samples.snl.flows.GameInfo;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collections;

public class FlowTests {
    private  MockNetwork network;
    private  StartedMockNode a;
    private  StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters(
                        ImmutableList.of(
                                TestCordapp.findCordapp("net.corda.samples.snl.flows"),
                                TestCordapp.findCordapp("net.corda.sample.snl.contracts"),
                                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                                TestCordapp.findCordapp("com.r3.corda.lib.ci"))
                ).withNetworkParameters(new NetworkParameters(4, Collections.emptyList(),
                        10485760, 10485760 * 50, Instant.now(), 1,
                        Collections.emptyMap()))
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
    public void testCreateBoardConfigFlow() throws Exception {
        CreateAccount createAccount1 = new CreateAccount("Ashutosh");
        a.startFlow(createAccount1);
        network.runNetwork();

        CreateAccount createAccount2 = new CreateAccount("Peter");
        a.startFlow(createAccount2);
        network.runNetwork();

        CreateBoardConfig.Initiator createBoardConfig = new CreateBoardConfig.Initiator("Ashutosh", "Peter");
        CordaFuture<SignedTransaction> signedTransactionCordaFuture = a.startFlow(createBoardConfig);
        network.runNetwork();

        SignedTransaction signedTransaction = signedTransactionCordaFuture.get();
        BoardConfig boardConfig = (BoardConfig) signedTransaction.getTx().getOutput(0);
        assertNotNull(boardConfig);
    }


    @Test
    public void testCreateGameFlow() throws Exception{
        CreateAccount createAccount1 = new CreateAccount("Ashutosh");
        a.startFlow(createAccount1);
        network.runNetwork();

        CreateAccount createAccount2 = new CreateAccount("Peter");
        a.startFlow(createAccount2);
        network.runNetwork();

        CreateBoardConfig.Initiator createBoardConfig = new CreateBoardConfig.Initiator("Ashutosh", "Peter");
        CordaFuture<SignedTransaction> signedTransactionCordaFuture = a.startFlow(createBoardConfig);
        network.runNetwork();

        CreateGameFlow.Initiator createGameFlow = new CreateGameFlow.Initiator("Ashutosh", "Peter");
        CordaFuture<String> signedTransactionCordaFuture1 = a.startFlow(createGameFlow);
        network.runNetwork();

        String gameId = signedTransactionCordaFuture1.get();
        assertNotNull(gameId);
    }
}
