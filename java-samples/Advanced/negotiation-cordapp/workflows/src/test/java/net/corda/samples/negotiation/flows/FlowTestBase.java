package net.corda.samples.negotiation.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.driver.VerifierType;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

abstract class FlowTestBase {
    protected MockNetwork network;
    protected StartedMockNode a;
    protected StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters(
                ImmutableList.of(
                        TestCordapp.findCordapp("net.corda.samples.negotiation.flows"),
                        TestCordapp.findCordapp("net.corda.samples.negotiation.contracts"))
        ).withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);

        List<Class> responseflows = ImmutableList.of(ProposalFlow.Responder.class, AcceptanceFlow.Responder.class, ModificationFlow.Responder.class);

        ImmutableList.of(a,b).forEach(node -> {
            for (Class flow:responseflows
                 ) {
                node.registerInitiatedFlow(flow);

            }
        });
        network.runNetwork();

    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    public UniqueIdentifier nodeACreatesProposal(Boolean isBuyer, int amount, Party counterparty) throws ExecutionException, InterruptedException {
        ProposalFlow.Initiator flow = new ProposalFlow.Initiator(isBuyer, amount, counterparty);
        Future future = a.startFlow(flow);
        network.runNetwork();
        return (UniqueIdentifier) future.get();

    }

    public void nodeBAcceptsProposal(UniqueIdentifier proposalId) throws ExecutionException, InterruptedException {
        AcceptanceFlow.Initiator flow = new AcceptanceFlow.Initiator(proposalId);
        Future future = b.startFlow(flow);
        network.runNetwork();
        future.get();
    }

    public void nodeBModifiesProposal(UniqueIdentifier proposalId, int newAmount) throws ExecutionException, InterruptedException {
        ModificationFlow.Initiator flow = new ModificationFlow.Initiator(proposalId, newAmount);
        Future future = b.startFlow(flow);
        network.runNetwork();
        future.get();
    }
}
