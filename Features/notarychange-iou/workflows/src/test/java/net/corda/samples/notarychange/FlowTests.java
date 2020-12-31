package net.corda.samples.notarychange;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.notarychange.flows.IssueFlow;
import net.corda.samples.notarychange.flows.SettleFlow;
import net.corda.samples.notarychange.flows.SwitchNotaryFlow;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import net.corda.testing.node.MockNetworkNotarySpec;
import org.junit.rules.ExpectedException;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters()
        .withCordappsForAllNodes(
                ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.notarychange.contracts"),
                TestCordapp.findCordapp("net.corda.samples.notarychange.flows")
                ))
            .withNotarySpecs(
                    Arrays.asList(
                            new MockNetworkNotarySpec(new CordaX500Name("NotaryA", "Toronto", "CA")),
                            new MockNetworkNotarySpec(new CordaX500Name("NotaryB", "Toronto", "CA"))))
    );

    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode Notary = network.createNode(new CordaX500Name("PartyA", "Toronto", "CA"));


    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void NoNotaryChangesExpectToFail() throws ExecutionException, InterruptedException {

        IssueFlow.Initiator issueflow = new IssueFlow.Initiator(20, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<String> future = a.startFlow(issueflow);
        network.runNetwork();
        String returnString = future.get();
        System.out.println("\n----------");
        System.out.println(returnString);

        String id = returnString.substring(returnString.indexOf("linearId: ") + 10);
        System.out.println(id);

        SettleFlow.Initiator settleflow = new SettleFlow.Initiator(UniqueIdentifier.Companion.fromString(id),
                network.getNotaryNodes().get(1).getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future2 = b.startFlow(settleflow);
        network.runNetwork();
        exception.expectCause(instanceOf(IllegalArgumentException.class));
        future2.get();
    }

    @Test
    public void CondunctNotaryChanges() throws ExecutionException, InterruptedException {

        IssueFlow.Initiator issueflow = new IssueFlow.Initiator(20, b.getInfo().getLegalIdentities().get(0));
        CordaFuture<String> future = a.startFlow(issueflow);
        network.runNetwork();
        String returnString = future.get();
        System.out.println("\n----------");
        System.out.println(returnString);

        String id = returnString.substring(returnString.indexOf("linearId: ") + 10);
        System.out.println(id);

        //notary change
        SwitchNotaryFlow notarychange = new SwitchNotaryFlow(UniqueIdentifier.Companion.fromString(id),
                network.getNotaryNodes().get(1).getInfo().getLegalIdentities().get(0));
        CordaFuture<String> future3 = b.startFlow(notarychange);
        network.runNetwork();

        //settle
        SettleFlow.Initiator settleflow = new SettleFlow.Initiator(UniqueIdentifier.Companion.fromString(id),
                network.getNotaryNodes().get(1).getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future2 = b.startFlow(settleflow);
        network.runNetwork();
        future2.get();
    }

}
