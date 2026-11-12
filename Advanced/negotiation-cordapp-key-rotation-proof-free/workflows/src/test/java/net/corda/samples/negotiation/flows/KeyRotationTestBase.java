package net.corda.samples.negotiation.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.negotiation.states.ProposalState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkNotarySpec;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A two-node Corda Enterprise mock network for the key rotation tests, plus one-line helpers to run the sample's flows.
 *
 * The network is created with {@code MockNetworkParameters.enterpriseNodes} set to true, so every node runs the Enterprise
 * versions of {@code CollectSignaturesFlow} and {@code FinalityFlow}, exactly like a real Corda Enterprise node; those are the
 * flows that know how to collect and verify a signature made with a rotated key. {@code MockNetwork.rotateKey} rotates a
 * node's legal identity key the way cross-provider key rotation does.
 */
abstract class KeyRotationTestBase {

    /** Corda Enterprise 4.15 is platform version 170; key rotation proofs need at least that on the network. */
    private static final int MINIMUM_PLATFORM_VERSION = 170;

    private static final List<Class<? extends FlowLogic<?>>> RESPONDER_FLOWS = ImmutableList.<Class<? extends FlowLogic<?>>>of(
            ProposalFlow.Responder.class, AcceptanceFlow.Responder.class, ModificationFlow.Responder.class);

    protected MockNetwork network;
    protected StartedMockNode a;
    protected StartedMockNode b;

    @Before
    public void setUp() {
        NetworkParameters networkParameters = new NetworkParameters(MINIMUM_PLATFORM_VERSION, Collections.emptyList(),
                10485760, 10485760 * 50, Instant.now(), 1, Collections.emptyMap());

        network = new MockNetwork(new MockNetworkParameters()
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
                .withNetworkParameters(networkParameters)
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp("net.corda.samples.negotiation.flows"),
                        TestCordapp.findCordapp("net.corda.samples.negotiation.contracts")))
                .withEnterpriseNodes(true));

        a = network.createPartyNode(new CordaX500Name("Node A", "London", "GB"));
        b = network.createPartyNode(new CordaX500Name("Node B", "New York", "US"));
        registerResponders(a);
        registerResponders(b);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        if (network != null) {
            network.stopNodes();
        }
    }

    /**
     * Rotates the node's legal identity key the way the Corda Enterprise cross-provider key rotation does: a new key is
     * generated, the OLD key signs the NEW public key (that signature is the rotation proof) and the node restarts on the new
     * key with the proof in its identity service. Use the returned node from here on; the one passed in is gone.
     */
    protected StartedMockNode rotate(StartedMockNode node) {
        StartedMockNode rotated = network.rotateKey(node, null);
        registerResponders(rotated);
        return rotated;
    }

    protected static Party identityOf(StartedMockNode node) {
        return node.getInfo().getLegalIdentities().get(0);
    }

    // --- The sample's flows, one call each -------------------------------------------------------------------------------

    /** {@code proposer} proposes to {@code counterparty}; the proposer is the buyer. Only the proposee can then modify or accept. */
    protected UniqueIdentifier propose(StartedMockNode proposer, int amount, Party counterparty) throws ExecutionException, InterruptedException {
        return run(proposer, new ProposalFlow.Initiator(true, amount, counterparty));
    }

    protected SignedTransaction modify(StartedMockNode node, UniqueIdentifier proposalId, int newAmount) throws ExecutionException, InterruptedException {
        return run(node, new ModificationFlow.Initiator(proposalId, newAmount));
    }

    protected SignedTransaction accept(StartedMockNode node, UniqueIdentifier proposalId) throws ExecutionException, InterruptedException {
        return run(node, new AcceptanceFlow.Initiator(proposalId));
    }

    /** The unconsumed proposal with this id, as recorded in the node's vault. */
    protected ProposalState proposalOf(StartedMockNode node, UniqueIdentifier proposalId) {
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(proposalId), Vault.StateStatus.UNCONSUMED, null);
        return node.transaction(() ->
                node.getServices().getVaultService().queryBy(ProposalState.class, criteria).getStates().get(0).getState().getData());
    }

    private <T> T run(StartedMockNode node, FlowLogic<T> flow) throws ExecutionException, InterruptedException {
        CordaFuture<T> result = node.startFlow(flow);
        network.runNetwork();
        return result.get();
    }

    private void registerResponders(StartedMockNode node) {
        for (Class<? extends FlowLogic<?>> responder : RESPONDER_FLOWS) {
            node.registerInitiatedFlow(responder);
        }
    }
}
