package net.corda.samples.tokentofriend;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.tokentofriend.flows.IssueToken;
import net.corda.samples.tokentofriend.states.CustomTokenState;
import net.corda.samples.tokentofriend.flows.CreateMyToken;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;
    private StartedMockNode d;
    private StartedMockNode e;

    private NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.tokentofriend.contracts"),
                TestCordapp.findCordapp("net.corda.samples.tokentofriend.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"))).withNetworkParameters(testNetworkParameters)
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
        );
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);
        d = network.createPartyNode(null);
        e = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void CheckTheCorrectMessageIsStored() throws ExecutionException, InterruptedException {
        String msg = "Test message";
        CreateMyToken flow = new CreateMyToken("1@gmail.com","2@gmail.com",msg);
        Future<UniqueIdentifier> future = a.startFlow(flow);
        network.runNetwork();
        UniqueIdentifier tokenStateID = future.get();
        CustomTokenState storedState = a.getServices().getVaultService().queryBy(CustomTokenState.class).getStates().get(0).getState().getData();
        assert (storedState.getMessage().equals(msg));
    }

    @Test
    public void CheckIfNonFungibleTokenCorrectlyCreated() throws ExecutionException, InterruptedException{
        String msg = "Test message";
        CreateMyToken flow = new CreateMyToken("1@gmail.com","2@gmail.com",msg);
        Future<UniqueIdentifier> future = a.startFlow(flow);
        network.runNetwork();
        UniqueIdentifier tokenStateID = future.get();
        IssueToken issueTokenflow = new IssueToken(tokenStateID.getId().toString());
        Future<String> future2 = a.startFlow(issueTokenflow);
        network.runNetwork();
        String resultString = future2.get();
        System.out.println(resultString);
        int subString = resultString.indexOf("Token Id is: ");
        String nonfungibleTokenId = resultString.substring(subString+13,resultString.indexOf("Storage Node is:")-1);
        System.out.println("-"+ nonfungibleTokenId+"-");
        List<StateAndRef<NonFungibleToken>> storedNonFungibleTokenb = b.getServices().getVaultService().queryBy(NonFungibleToken.class).getStates();
        List<StateAndRef<NonFungibleToken>> storedNonFungibleTokenc = c.getServices().getVaultService().queryBy(NonFungibleToken.class).getStates();
        List<StateAndRef<NonFungibleToken>> storedNonFungibleTokend = d.getServices().getVaultService().queryBy(NonFungibleToken.class).getStates();
        List<StateAndRef<NonFungibleToken>> storedNonFungibleTokene = e.getServices().getVaultService().queryBy(NonFungibleToken.class).getStates();
        NonFungibleToken storedToken = Arrays.asList(storedNonFungibleTokenb,storedNonFungibleTokenc,storedNonFungibleTokend,storedNonFungibleTokene).stream().filter(it ->
                     0 != it.size()
                ).collect(Collectors.toList()).get(0).get(0).getState().getData();
        System.out.println("-"+ storedToken.getLinearId().toString()+"-");
        assert (storedToken.getLinearId().toString().equals(nonfungibleTokenId));
    }

}
