package net.corda.samples.duediligence;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.samples.duediligence.flows.RequestToValidateCorporateRecords;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import static org.jgroups.util.Util.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.duediligence.contracts"),
                TestCordapp.findCordapp("net.corda.samples.duediligence.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void CreationAndSigningTheAuditingRequest() throws ExecutionException, InterruptedException {
        RequestToValidateCorporateRecords.RequestToValidateCorporateRecordsInitiator flow1 =
                new RequestToValidateCorporateRecords.RequestToValidateCorporateRecordsInitiator(b.getInfo().getLegalIdentities().get(0),10);
        Future<String> future = a.startFlow(flow1);
        network.runNetwork();
        String result1 = future.get();
        System.out.println(result1);
        int subString = result1.indexOf("Case Id: ");
        String ApproalID = result1.substring(subString+9);
        System.out.println("-"+ ApproalID+"-");

        UniqueIdentifier id = UniqueIdentifier.Companion.fromString(ApproalID);
        //Query the input
        QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(id.toString())))
                .withStatus(Vault.StateStatus.UNCONSUMED)
                .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
        StateAndRef inputStateAndRef = a.getServices().getVaultService().queryBy(ContractState.class, inputCriteria).getStates().get(0);
        CorporateRecordsAuditRequest result = (CorporateRecordsAuditRequest)inputStateAndRef.getState().getData();
        assertEquals(result.getLinearId(),id);
    }
}
