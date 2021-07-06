package net.corda.samples.election;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.NetworkParameters;
import net.corda.samples.election.accountUtilities.CreateNewAccount;
import net.corda.samples.election.accountUtilities.ShareAccountTo;
import net.corda.samples.election.accountUtilities.CountVotes;
import net.corda.samples.election.flows.SendVote;
import net.corda.samples.election.states.VoteState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.corda.testing.node.StartedMockNode;
import java.util.*;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode o;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    private NetworkParameters testNetworkParameters =
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
        CreateNewAccount createAcct = new CreateNewAccount("TestAccountA");
        Future<String> future = a.startFlow(createAcct);
        network.runNetwork();
        AccountService accountService = a.getServices().cordaService(KeyManagementBackedAccountService.class);
        List<StateAndRef<AccountInfo>> myAccount = accountService.accountInfo("TestAccountA");
        assert (myAccount.size() != 0);
    }

    @Test
    public void VoteFlowTest() throws ExecutionException, InterruptedException {
        CreateNewAccount createAcct = new CreateNewAccount("TestAccountA");
        Future<String> future = a.startFlow(createAcct);
        network.runNetwork();
        ShareAccountTo shareAToO = new ShareAccountTo("TestAccountA",o.getInfo().getLegalIdentities().get(0));
        Future<String> future2 = a.startFlow(shareAToO);
        network.runNetwork();

        CreateNewAccount createAcct2 = new CreateNewAccount("TestAccountB");
        Future<String> future3 = b.startFlow(createAcct2);
        network.runNetwork();
        ShareAccountTo shareBToO = new ShareAccountTo("TestAccountB",o.getInfo().getLegalIdentities().get(0));
        Future<String> future4 = b.startFlow(shareBToO);
        network.runNetwork();

        SendVote voteflow = new SendVote("TestAccountA",o.getInfo().getLegalIdentities().get(0),2);
        Future<String> future5 = a.startFlow(voteflow);
        network.runNetwork();

        SendVote voteflow2 = new SendVote("TestAccountB",o.getInfo().getLegalIdentities().get(0),2);
        Future<String> future6 = b.startFlow(voteflow2);
        network.runNetwork();

        //retrieve
        CountVotes countvotes = new CountVotes();
        CordaFuture<List<Integer>> votes = o.startFlow(countvotes);
        network.runNetwork();

        List<Integer> checkArray = new ArrayList<Integer>(Arrays.asList(0, 0, 2, 0, 0, 0, 0, 0, 0, 0));
//        System.out.println(votes.get());
//        System.out.println(checkArray);
        assert votes.get().equals(checkArray);
    }
}
