package net.corda.samples.lending;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.lending.flows.ApproveLoanBidFlow;
import net.corda.samples.lending.flows.SubmitLoanBidFlow;
import net.corda.samples.lending.flows.SubmitProjectProposalFlow;
import net.corda.samples.lending.states.LoanBidState;
import net.corda.samples.lending.states.ProjectState;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;
    private StartedMockNode d;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.lending.contracts"),
                TestCordapp.findCordapp("net.corda.samples.lending.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);
        d = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    private ProjectState createProject() throws Exception{
        SubmitProjectProposalFlow.Initiator flow = new SubmitProjectProposalFlow.Initiator(
                Arrays.asList(
                        b.getInfo().getLegalIdentities().get(0),
                        c.getInfo().getLegalIdentities().get(0)),
                "Test Project", 10000000, 8000000);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction transaction = future.get();
        ProjectState projectState = (ProjectState) transaction.getTx().getOutput(0);
        return  projectState;
    }

    private LoanBidState submitLoanBid(ProjectState projectState) throws Exception{
        SubmitLoanBidFlow.Initiator flow = new SubmitLoanBidFlow.Initiator(
                projectState.getBorrower(),
                8000000, 5, 8, 20000,
                projectState.getLinearId()
        );
        CordaFuture<SignedTransaction> future = b.startFlow(flow);
        network.runNetwork();
        SignedTransaction transaction = future.get();
        LoanBidState loanBidState = (LoanBidState) transaction.getTx().getOutput(0);
        return  loanBidState;
    }

    private LoanBidState approveLoanBid(LoanBidState loanBidState) throws Exception{
        ApproveLoanBidFlow.Initiator flow = new ApproveLoanBidFlow.Initiator(loanBidState.getLinearId());
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction transaction = future.get();
        LoanBidState loanBidStateUpdated = (LoanBidState) transaction.getTx().getOutput(0);
        return  loanBidStateUpdated;
    }

    @Test
    public void testSubmitProjectProposalFlow() throws Exception{
        ProjectState projectState = createProject();
        assertNotNull(projectState);
    }

    @Test
    public void testSubmitLoanBidFlow() throws Exception{
        ProjectState projectState = createProject();
        LoanBidState loanBidState = submitLoanBid(projectState);
        assertNotNull(loanBidState);
    }

    @Test
    public void testApproveLoanBidFlow() throws Exception{
        ProjectState projectState = createProject();
        LoanBidState loanBidState = submitLoanBid(projectState);
        LoanBidState loanBidStateApproved = approveLoanBid(loanBidState);
        assertNotNull(loanBidStateApproved);
        assertEquals("APPROVED", loanBidStateApproved.getStatus());
    }


}
