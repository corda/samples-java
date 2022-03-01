package net.corda.samples.lending;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.lending.flows.*;
import net.corda.samples.lending.states.LoanBidState;
import net.corda.samples.lending.states.ProjectState;
import net.corda.samples.lending.states.SyndicateBidState;
import net.corda.samples.lending.states.SyndicateState;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class AllInOneTester {

    private MockNetwork network;
    private StartedMockNode bankA;
    private StartedMockNode bankB;
    private StartedMockNode bankC;
    private StartedMockNode borrower;
    private NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());
    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.lending.contracts"),
                TestCordapp.findCordapp("net.corda.samples.lending.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
        .withNetworkParameters(testNetworkParameters));
        borrower = network.createPartyNode(null);
        bankA = network.createPartyNode(null);
        bankB = network.createPartyNode(null);
        bankC = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void AllBusinessLogicFlowTests() {
        //Create the project
        List<Party> lenders = new ArrayList<Party>();
        lenders.add(bankA.getInfo().getLegalIdentities().get(0));
        lenders.add(bankB.getInfo().getLegalIdentities().get(0));
        SubmitProjectProposalFlow.Initiator createProjectFlow = new SubmitProjectProposalFlow.Initiator(lenders, "oversea expansion", 100, 10);
        Future<SignedTransaction> future = borrower.startFlow(createProjectFlow);
        network.runNetwork();

        //successful query means the state is stored at node b's vault. Flow went through.
        QueryCriteria inputCriteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED);
        ProjectState project = bankA.getServices().getVaultService().queryBy(ProjectState.class,inputCriteria).getStates().get(0).getState().getData();

        //place loan bid
        UniqueIdentifier projectID = project.getLinearId();
        SubmitLoanBidFlow.Initiator submitLoanBidFlow = new SubmitLoanBidFlow.Initiator(borrower.getInfo().getLegalIdentities().get(0), 7, 5, 4.0, 1, projectID);
        Future<SignedTransaction> future2 = bankA.startFlow(submitLoanBidFlow);
        network.runNetwork();
        LoanBidState loanBid = borrower.getServices().getVaultService().queryBy(LoanBidState.class,inputCriteria).getStates().get(0).getState().getData();

        //Borrower approves the loan bid. pick the winning bank
        UniqueIdentifier loanBidId = loanBid.getLinearId();
        ApproveLoanBidFlow.Initiator approveLoanBidFlow = new ApproveLoanBidFlow.Initiator(loanBidId);
        Future<SignedTransaction> future3 = borrower.startFlow(approveLoanBidFlow);
        network.runNetwork();
        LoanBidState loanBidApproved = bankA.getServices().getVaultService().queryBy(LoanBidState.class,inputCriteria).getStates().get(0).getState().getData();
        assertEquals("APPROVED", loanBidApproved.getStatus());

        //lead bank create Syndication
        UniqueIdentifier approveLoanBidID = loanBidApproved.getLinearId();
        List<Party> participatingBanks = new ArrayList<Party>();
        participatingBanks.add(bankB.getInfo().getLegalIdentities().get(0));
        participatingBanks.add(bankC.getInfo().getLegalIdentities().get(0));
        CreateSyndicateFlow.Initiator createSyndication = new CreateSyndicateFlow.Initiator(participatingBanks, projectID, approveLoanBidID);
        Future<SignedTransaction> future4 = bankA.startFlow(createSyndication);
        network.runNetwork();
        SyndicateState syndication = bankA.getServices().getVaultService().queryBy(SyndicateState.class,inputCriteria).getStates().get(0).getState().getData();

        //participating bank submit syndication bid
        UniqueIdentifier syndicationID = syndication.getLinearId();
        SubmitSyndicateBidFlow.Initiator submitSyndicationBid = new SubmitSyndicateBidFlow.Initiator(syndicationID,2);
        Future<SignedTransaction> future5 = bankB.startFlow(submitSyndicationBid);
        network.runNetwork();
        SyndicateBidState syndicatedBid = bankA.getServices().getVaultService().queryBy(SyndicateBidState.class,inputCriteria).getStates().get(0).getState().getData();

        //Lead bank approves syndication bid
        UniqueIdentifier syndicationBidId = syndicatedBid.getLinearId();
        ApproveSyndicateBidFlow.Initiator approveSyndicationBid = new ApproveSyndicateBidFlow.Initiator(syndicationBidId);
        Future<SignedTransaction> future6 = bankA.startFlow(approveSyndicationBid);
        network.runNetwork();
        SyndicateBidState syndicatedBidApproved = bankA.getServices().getVaultService().queryBy(SyndicateBidState.class,inputCriteria).getStates().get(0).getState().getData();
        assertEquals("APPROVED", syndicatedBidApproved.getStatus());

    }
}
