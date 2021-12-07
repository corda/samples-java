package net.corda.samples.autopayroll;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.autopayroll.flows.RequestFlow;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters()
        .withCordappsForAllNodes(
                ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.autopayroll.contracts"),
                TestCordapp.findCordapp("net.corda.samples.autopayroll.flows")
                )
        )
            .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
    );

    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode bank = network.createNode(new CordaX500Name("BankOperator", "Toronto", "CA"));

    public FlowTests() {
        ImmutableList.of(a, b).forEach((it) -> {
            it.registerInitiatedFlow(RequestFlow.RequestFlowResponder.class);
        });
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    //Test #1 check if the requestState is being sent to the bank operator behind teh scenes
    @Test
    public void requestStateSent() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new RequestFlow.RequestFlowInitiator("500", b.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();

        SignedTransaction ptx = future.get();
        System.out.println("Signed transaction hash: " + ptx.getId());
        ImmutableList.of(a, bank)
                .stream()
                .map(it -> it.getServices().getValidatedTransactions().getTransaction(ptx.getId()))
                .collect(Collectors.toList())
                .forEach(it -> {
                    SecureHash txHash = it.getId();
                    System.out.println(txHash + " == " + ptx.getId());
                    assertEquals(ptx.getId(), txHash);
                });
    }

}
