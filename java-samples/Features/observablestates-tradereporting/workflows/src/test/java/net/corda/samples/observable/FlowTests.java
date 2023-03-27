package net.corda.samples.observable;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.observable.flows.ReportManuallyResponder;
import net.corda.samples.observable.flows.TradeAndReport;
import net.corda.samples.observable.flows.TradeAndReportResponder;
import net.corda.samples.observable.states.HighlyRegulatedState;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.corda.core.crypto.SecureHash;

import java.util.concurrent.ExecutionException;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters()
            .withCordappsForAllNodes(
                    ImmutableList.of(
                            TestCordapp.findCordapp("net.corda.samples.observable.contracts"),
                            TestCordapp.findCordapp("net.corda.samples.observable.flows")
                    )
            ).withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
    );

    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode c = network.createNode();
    private final StartedMockNode d = network.createNode();

    @Before
    public void setup() {

        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(a, b,c,d)) {
            node.registerInitiatedFlow(TradeAndReportResponder.class);
            node.registerInitiatedFlow(ReportManuallyResponder.class);
        }network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void CheckIfObserverHaveTheStates() throws ExecutionException, InterruptedException {
        a.startFlow(new TradeAndReport(d.getInfo().getLegalIdentities().get(0),
                b.getInfo().getLegalIdentities().get(0),c.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        HighlyRegulatedState bNodeStoredStates = b.getServices().getVaultService().queryBy(HighlyRegulatedState.class)
                .getStates().get(0).getState().getData();
        assert (bNodeStoredStates.getParticipants().contains(d.getInfo().getLegalIdentities().get(0)));
        HighlyRegulatedState cNodeStoredStates = c.getServices().getVaultService().queryBy(HighlyRegulatedState.class)
                .getStates().get(0).getState().getData();
        assert (cNodeStoredStates.getParticipants().contains(d.getInfo().getLegalIdentities().get(0)));
    }
}
