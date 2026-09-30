package net.corda.samples.negotiation.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.negotiation.states.ProposalState;
import net.corda.testing.node.InMemoryMessagingNetwork;
import net.corda.testing.node.MockNetworkNotarySpec;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.internal.DriverDSLImplKt;
import net.corda.testing.node.internal.InternalMockNetwork;
import net.corda.testing.node.internal.InternalTestUtilsKt;
import net.corda.testing.node.internal.TestCordappInternal;
import net.corda.testing.node.internal.TestStartedNode;
import org.junit.After;
import org.junit.Before;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A two-node Corda Enterprise mock network for the key rotation tests, plus one-line helpers to run the sample's flows.
 *
 * The nodes are created with {@code InternalMockNetwork.createKeyRotationMockNode}, which is what the Corda Enterprise
 * repository's own key rotation tests use: such a node runs the Enterprise versions of {@code CollectSignaturesFlow} and
 * {@code FinalityFlow}, exactly like a real Enterprise node, and those are the flows that know how to collect and verify a
 * signature made with a rotated key. The public {@code MockNetwork} creates plain nodes running the Corda OS flows, which do not.
 */
abstract class KeyRotationTestBase {

    /** Corda Enterprise 4.15 is platform version 170; key rotation proofs need at least that on the network. */
    private static final int MINIMUM_PLATFORM_VERSION = 170;

    private static final List<Class<? extends FlowLogic<?>>> RESPONDER_FLOWS = ImmutableList.<Class<? extends FlowLogic<?>>>of(
            ProposalFlow.Responder.class, AcceptanceFlow.Responder.class, ModificationFlow.Responder.class);

    protected InternalMockNetwork network;
    protected TestStartedNode a;
    protected TestStartedNode b;

    @Before
    public void setUp() {
        NetworkParameters networkParameters = new NetworkParameters(MINIMUM_PLATFORM_VERSION, Collections.emptyList(),
                10485760, 10485760 * 50, Instant.now(), 1, Collections.emptyMap());

        network = new InternalMockNetwork(
                Collections.emptyList(),
                new MockNetworkParameters(),
                false,
                false,
                new InMemoryMessagingNetwork.ServicePeerAllocationStrategy.Random(),
                ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))),
                Paths.get("build", "mock-network", DriverDSLImplKt.getTimestampAsDirectoryName()),
                networkParameters,
                InternalMockNetwork.Companion::createKeyRotationMockNode,
                ImmutableList.of(
                        (TestCordappInternal) TestCordapp.findCordapp("net.corda.samples.negotiation.flows"),
                        (TestCordappInternal) TestCordapp.findCordapp("net.corda.samples.negotiation.contracts")),
                true);

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
    protected TestStartedNode rotate(TestStartedNode node) {
        TestStartedNode rotated = network.rotateKey(node, null);
        registerResponders(rotated);
        return rotated;
    }

    protected static Party identityOf(TestStartedNode node) {
        return node.getInfo().getLegalIdentities().get(0);
    }

    // --- The sample's flows, one call each -------------------------------------------------------------------------------

    /** {@code proposer} proposes to {@code counterparty}; the proposer is the buyer. Only the proposee can then modify or accept. */
    protected UniqueIdentifier propose(TestStartedNode proposer, int amount, Party counterparty) throws ExecutionException, InterruptedException {
        return run(proposer, new ProposalFlow.Initiator(true, amount, counterparty));
    }

    protected SignedTransaction modify(TestStartedNode node, UniqueIdentifier proposalId, int newAmount) throws ExecutionException, InterruptedException {
        return run(node, new ModificationFlow.Initiator(proposalId, newAmount));
    }

    protected SignedTransaction accept(TestStartedNode node, UniqueIdentifier proposalId) throws ExecutionException, InterruptedException {
        return run(node, new AcceptanceFlow.Initiator(proposalId));
    }

    /** The unconsumed proposal with this id, as recorded in the node's vault. */
    protected ProposalState proposalOf(TestStartedNode node, UniqueIdentifier proposalId) {
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(proposalId), Vault.StateStatus.UNCONSUMED, null);
        return node.getDatabase().transaction(tx ->
                node.getServices().getVaultService().queryBy(ProposalState.class, criteria).getStates().get(0).getState().getData());
    }

    private <T> T run(TestStartedNode node, FlowLogic<T> flow) throws ExecutionException, InterruptedException {
        Future<T> result = InternalTestUtilsKt.startFlow(node.getServices(), flow).getResultFuture();
        network.runNetwork();
        return result.get();
    }

    private void registerResponders(TestStartedNode node) {
        for (Class<? extends FlowLogic<?>> responder : RESPONDER_FLOWS) {
            node.registerInitiatedFlow(responder, false);
        }
    }
}
