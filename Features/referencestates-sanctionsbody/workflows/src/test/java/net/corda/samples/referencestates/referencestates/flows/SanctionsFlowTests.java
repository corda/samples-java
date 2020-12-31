package net.corda.samples.referencestates.referencestates.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.samples.referencestates.states.SanctionedEntities;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SanctionsFlowTests {
    MockNetwork network;
    StartedMockNode a;
    StartedMockNode b;
    StartedMockNode c;

    @Before
    public void setup() {
        network = new MockNetwork(
                ImmutableList.of("net.corda.samples.referencestates.contracts"));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);

        ImmutableList.of(a, b).forEach(node -> {
            node.registerInitiatedFlow(GetSanctionsListFlow.Acceptor.class);
        });


    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void issueSanctionsListWithExpectedIssuer() throws ExecutionException, InterruptedException {
        IssueSanctionsListFlow.Initiator flow = new IssueSanctionsListFlow.Initiator();
        Future future = a.startFlow(flow);
        network.runNetwork();
        StateAndRef futureData = (StateAndRef) future.get();

        Assert.assertEquals(a.getInfo().getLegalIdentities().get(0), ((SanctionedEntities) futureData.getState().getData()).getIssuer());
    }

    @Test
    public void collectNewestSanctionsList() throws ExecutionException, InterruptedException {
        IssueSanctionsListFlow.Initiator issueFlow = new IssueSanctionsListFlow.Initiator();
        Future issueListFuture = a.startFlow(issueFlow);
        network.runNetwork();
        StateAndRef issueListFutureData = (StateAndRef) issueListFuture.get();
        Assert.assertEquals(a.getInfo().getLegalIdentities().get(0), ((SanctionedEntities) issueListFutureData.getState().getData()).getIssuer());

        GetSanctionsListFlow.Initiator getFlow = new GetSanctionsListFlow.Initiator(a.getInfo().getLegalIdentities().get(0));
        Future getListFuture = b.startFlow(getFlow);
        network.runNetwork();
        List getListFutureList = (ArrayList) getListFuture.get();
        System.out.println(getListFutureList.size());
        StateAndRef getListFutureData = (StateAndRef) getListFutureList.get(0);

        Assert.assertEquals(((SanctionedEntities) issueListFutureData.getState().getData()).getLinearId(), ((SanctionedEntities) getListFutureData.getState().getData()).getLinearId());

    }


    @Test
    public void updatesSanctionsListWithNewSanctionee() throws ExecutionException, InterruptedException {
        IssueSanctionsListFlow.Initiator issueFlow = new IssueSanctionsListFlow.Initiator();
        Future issueListFuture = a.startFlow(issueFlow);
        network.runNetwork();
        StateAndRef issueListFutureData = (StateAndRef) issueListFuture.get();
        Assert.assertEquals(a.getInfo().getLegalIdentities().get(0), ((SanctionedEntities) issueListFutureData.getState().getData()).getIssuer());

        GetSanctionsListFlow.Initiator getFlow = new GetSanctionsListFlow.Initiator(a.getInfo().getLegalIdentities().get(0));
        Future getListFuture = b.startFlow(getFlow);
        network.runNetwork();
        List getListFutureList = (ArrayList) getListFuture.get();
        System.out.println(getListFutureList.size());
        StateAndRef getListFutureData = (StateAndRef) getListFutureList.get(0);

        Assert.assertEquals(((SanctionedEntities) issueListFutureData.getState().getData()).getLinearId(), ((SanctionedEntities) getListFutureData.getState().getData()).getLinearId());

        UpdateSanctionsListFlow.Initiator updateFlow = new UpdateSanctionsListFlow.Initiator(c.getInfo().getLegalIdentities().get(0));
        Future updateListFuture = a.startFlow(updateFlow);
        network.runNetwork();
        StateAndRef updateListFutureData = (StateAndRef) updateListFuture.get();

        Assert.assertTrue(((SanctionedEntities) updateListFutureData.getState().getData()).getBadPeople().contains(c.getInfo().getLegalIdentities().get(0)));

        GetSanctionsListFlow.Initiator getUpdatedFlow = new GetSanctionsListFlow.Initiator(a.getInfo().getLegalIdentities().get(0));
        Future getUpdatedList = b.startFlow(getUpdatedFlow);
        network.runNetwork();
        List getListUpdatedList = (ArrayList) getUpdatedList.get();
        StateAndRef getUpdatedListFutureData = (StateAndRef) getListUpdatedList.get(0);

        Assert.assertEquals(((SanctionedEntities) updateListFutureData.getState().getData()).getLinearId(), ((SanctionedEntities) getUpdatedListFutureData.getState().getData()).getLinearId());


    }
}
