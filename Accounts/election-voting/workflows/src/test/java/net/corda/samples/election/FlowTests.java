package net.corda.samples.election;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.NetworkParameters;
import net.corda.samples.election.accountUtilities.CountVotes;
import net.corda.samples.election.accountUtilities.CreateNewAccount;
import net.corda.samples.election.accountUtilities.ShareAccountTo;
import net.corda.samples.election.flows.SendVote;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode o;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    private final NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.election.contracts"),
                TestCordapp.findCordapp("net.corda.samples.election.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"))).withNetworkParameters(testNetworkParameters));
        o = network.createPartyNode(null);
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void AccountCreation() throws ExecutionException, InterruptedException {
        CreateNewAccount createAcct = new CreateNewAccount("alice-alice");
        Future<String> future = a.startFlow(createAcct);
        network.runNetwork();
        AccountService accountService = a.getServices().cordaService(KeyManagementBackedAccountService.class);
        // check that the account hash matches as the name
        List<StateAndRef<AccountInfo>> myAccount = accountService.accountInfo("11714");
        assert (myAccount.size() != 0);
    }

    @Test
    public void VoteFlowTest() throws ExecutionException, InterruptedException {
        //create accounts
        Future<String> future = null;
        //accounts for node A
        CreateNewAccount createAcctAA = new CreateNewAccount("alice-alice");
        future = a.startFlow(createAcctAA);
        network.runNetwork();
        ShareAccountTo shareAAToO = new ShareAccountTo("alice-alice",o.getInfo().getLegalIdentities().get(0));
        future = a.startFlow(shareAAToO);
        network.runNetwork();

        CreateNewAccount createAcctAB = new CreateNewAccount("alice-bob");
        future = a.startFlow(createAcctAB);
        network.runNetwork();
        ShareAccountTo shareABToO = new ShareAccountTo("alice-bob",o.getInfo().getLegalIdentities().get(0));
        future = a.startFlow(shareABToO);
        network.runNetwork();

        CreateNewAccount createAcctAC = new CreateNewAccount("alice-charlie");
        future = a.startFlow(createAcctAC);
        network.runNetwork();
        ShareAccountTo shareACToO = new ShareAccountTo("alice-charlie",o.getInfo().getLegalIdentities().get(0));
        future = a.startFlow(shareACToO);
        network.runNetwork();

        //accounts for node B
        CreateNewAccount createAcctBA = new CreateNewAccount("bob-alice");
        future = b.startFlow(createAcctBA);
        network.runNetwork();
        ShareAccountTo shareBAToO = new ShareAccountTo("bob-alice",o.getInfo().getLegalIdentities().get(0));
        future = b.startFlow(shareBAToO);
        network.runNetwork();

        CreateNewAccount createAcctBB = new CreateNewAccount("bob-bob");
        future = b.startFlow(createAcctBB);
        network.runNetwork();
        ShareAccountTo shareBBToO = new ShareAccountTo("bob-bob",o.getInfo().getLegalIdentities().get(0));
        future = b.startFlow(shareBBToO);
        network.runNetwork();

        CreateNewAccount createAcctBC = new CreateNewAccount("bob-charlie");
        future = b.startFlow(createAcctBC);
        network.runNetwork();
        ShareAccountTo shareBCToO = new ShareAccountTo("bob-charlie",o.getInfo().getLegalIdentities().get(0));
        future = b.startFlow(shareBCToO);
        network.runNetwork();

        //accounts for node C
        CreateNewAccount createAcctCA = new CreateNewAccount("charlie-alice");
        future = c.startFlow(createAcctCA);
        network.runNetwork();
        ShareAccountTo shareCAToO = new ShareAccountTo("charlie-alice",o.getInfo().getLegalIdentities().get(0));
        future = c.startFlow(shareCAToO);
        network.runNetwork();

        CreateNewAccount createAcctCB = new CreateNewAccount("charlie-bob");
        future = c.startFlow(createAcctCB);
        network.runNetwork();
        ShareAccountTo shareCBToO = new ShareAccountTo("charlie-bob",o.getInfo().getLegalIdentities().get(0));
        future = c.startFlow(shareCBToO);
        network.runNetwork();

        CreateNewAccount createAcctCC = new CreateNewAccount("charlie-charlie");
        future = c.startFlow(createAcctCC);
        network.runNetwork();
        ShareAccountTo shareCCToO = new ShareAccountTo("charlie-charlie",o.getInfo().getLegalIdentities().get(0));
        future = c.startFlow(shareCCToO);
        network.runNetwork();

        System.out.println("ACCOUNTS CREATED AND SHARED");

        //send votes
        // votes for node A's accounts
        SendVote voteFlowAA = new SendVote("alice-alice",o.getInfo().getLegalIdentities().get(0),"1970", 5);
        future = a.startFlow(voteFlowAA);
        network.runNetwork();

        SendVote voteFlowAB = new SendVote("alice-bob",o.getInfo().getLegalIdentities().get(0),"1970", 1);
        future = a.startFlow(voteFlowAB);
        network.runNetwork();

        SendVote voteFlowAC = new SendVote("alice-charlie",o.getInfo().getLegalIdentities().get(0),"1970", 2);
        future = a.startFlow(voteFlowAC);
        network.runNetwork();

        // votes for node B's accounts
        SendVote voteFlowBA = new SendVote("bob-alice",o.getInfo().getLegalIdentities().get(0),"1970", 0);
        future = b.startFlow(voteFlowBA);
        network.runNetwork();

        SendVote voteFlowBB = new SendVote("bob-bob",o.getInfo().getLegalIdentities().get(0),"1970", 1);
        future = b.startFlow(voteFlowBB);
        network.runNetwork();

        SendVote voteFlowBC = new SendVote("bob-charlie",o.getInfo().getLegalIdentities().get(0),"1970", 1);
        future = b.startFlow(voteFlowBC);
        network.runNetwork();

        // votes for node B's accounts
        SendVote voteFlowCA = new SendVote("charlie-alice",o.getInfo().getLegalIdentities().get(0),"1970", 2);
        future = c.startFlow(voteFlowCA);
        network.runNetwork();

        SendVote voteFlowCB = new SendVote("charlie-bob",o.getInfo().getLegalIdentities().get(0),"1970", 1);
        future = c.startFlow(voteFlowCB);
        network.runNetwork();

        SendVote voteFlowCC = new SendVote("charlie-charlie",o.getInfo().getLegalIdentities().get(0),"1970", 0);
        future = c.startFlow(voteFlowCC);
        network.runNetwork();

        //second vote from the same account, when counting votes, most recent vote (this one) will be recorded as the only one
        SendVote voteFlowAA2 = new SendVote("alice-alice",o.getInfo().getLegalIdentities().get(0),"1970", 0);
        future = a.startFlow(voteFlowAA2);
        network.runNetwork();

        //voting opportunity in a different election won't be counted in the 1970 election counts
        SendVote voteFlowBA2 = new SendVote("bob-alice",o.getInfo().getLegalIdentities().get(0),"1971", 6);
        future = b.startFlow(voteFlowBA2);
        network.runNetwork();

        System.out.println("VOTES SENT");

        //retrieve
        CountVotes countvotes = new CountVotes("1970");
        CordaFuture<List<Integer>> votes = o.startFlow(countvotes);
        network.runNetwork();

        List<Integer> checkArray = new ArrayList<Integer>(Arrays.asList(3, 4, 2, 0, 0, 0, 0, 0, 0, 0));
        System.out.println("OUTPUT: ");
        System.out.println(votes.get());
        System.out.println("COMPARED WITH: ");
        System.out.println(checkArray);
        assert votes.get().equals(checkArray);
    }
}
