package net.corda.samples.flow;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.Currencies;
import net.corda.testing.node.*;
import net.corda.samples.contracts.IOUContract;
import net.corda.samples.flows.IOUIssueFlow;
import net.corda.samples.flows.IOUTransferFlow;
import net.corda.samples.states.IOUState;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class IOUTransferFlowTests {

    private MockNetwork mockNetwork;
    private StartedMockNode a, b, c;

    @Before
    public void setup() {
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters().withCordappsForAllNodes(
                Arrays.asList(
                        TestCordapp.findCordapp("net.corda.samples.contracts")
                )
        ).withNotarySpecs(Arrays.asList(new MockNetworkNotarySpec(new CordaX500Name("Notary", "London", "GB"))));
        mockNetwork = new MockNetwork(mockNetworkParameters);
        System.out.println(mockNetwork);

        a = mockNetwork.createNode(new MockNodeParameters());
        b = mockNetwork.createNode(new MockNodeParameters());
        c = mockNetwork.createNode(new MockNodeParameters());

        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(a);
        startedNodes.add(b);
        startedNodes.add(c);

        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach(el -> el.registerInitiatedFlow(IOUTransferFlow.Responder.class));
        startedNodes.forEach(el -> el.registerInitiatedFlow(IOUIssueFlow.ResponderFlow.class));
        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private SignedTransaction issueIOU(IOUState iouState) throws InterruptedException, ExecutionException {
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iouState);
        CordaFuture future = a.startFlow(flow);
        mockNetwork.runNetwork();
        return (SignedTransaction) future.get();
    }

    /**
     * Task 1.
     * Build out the beginnings of [IOUTransferFlow]!
     * TODO: Implement the [IOUTransferFlow] flow which builds and returns a partially [SignedTransaction].
     * Hint:
     * - This flow will look similar to the [IOUIssueFlow].
     * - This time our transaction has an input state, so we need to retrieve it from the vault!
     * - You can use the [getServiceHub().getVaultService().queryBy(Class, queryCriteria)] method to get the latest linear states of a particular
     *   type from the vault. It returns a list of states matching your query.
     * - Use the [UniqueIdentifier] which is passed into the flow to create the appropriate Query Criteria.
     * - Use the [IOUState.withNewLender] method to create a copy of the state with a new lender.
     * - Create a Command - we will need to use the Transfer command.
     * - Remember, as we are involving three parties we will need to collect three signatures, so need to add three
     *   [PublicKey]s to the Command's signers list. We can get the signers from the input IOU and the new IOU you
     *   have just created with the new lender.
     * - Verify and sign the transaction as you did with the [IOUIssueFlow].
     * - Return the partially signed transaction.
     */
    @Test
    public void flowReturnsCorrectlyFormedPartiallySignedTransaction() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().getLegalIdentities().get(0));
        Future<SignedTransaction> future = a.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction ptx = future.get();

        // Check the transaction is well formed...
        // One output IOUState, one input state reference and a Transfer command with the right properties.
        assert (ptx.getTx().getInputs().size() == 1);
        assert (ptx.getTx().getOutputs().size() == 1);
        assert (ptx.getTx().getOutputs().get(0).getData() instanceof IOUState);
        assert (ptx.getTx().getInputs().get(0).equals(new StateRef(stx.getId(), 0)));

        IOUState outputIOU = (IOUState) ptx.getTx().getOutput(0);
        Command command = ptx.getTx().getCommands().get(0);

        assert (command.getValue().equals(new IOUContract.Commands.Transfer()));
        ptx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey(), c.getInfo().getLegalIdentities().get(0).getOwningKey(), mockNetwork.getDefaultNotaryIdentity().getOwningKey());
    }

    /**
     * Task 2.
     * We need to make sure that only the current lender can execute this flow.
     * TODO: Amend the [IOUTransferFlow] to only allow the current lender to execute the flow.
     * Hint:
     * - Remember: You can use the node's identity and compare it to the [Party] object within the [IOUState] you
     *   retrieved from the vault.
     * - Throw an [IllegalArgumentException] if the wrong party attempts to run the flow!
     */
    @Test
    public void flowCanOnlyBeRunByCurrentLender() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().component2().get(0).getParty());
        Future<SignedTransaction> future = b.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
        } catch (Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: This flow must be run by the current lender.");
        }
    }

    /**
     * Task 3.
     * Check that an [IOUState] cannot be transferred to the same lender.
     * TODO: You shouldn't have to do anything additional to get this test to pass. Belts and Braces!
     */
    @Test
    public void iouCannotBeTransferredToSameParty() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().component2().get(0).getParty());
        Future<SignedTransaction> future = a.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            assert exception.getMessage().equals("Contract verification failed: Failed requirement: The lender property must change in a transfer.");
        }
    }

    /**
     * Task 4.
     * Get the borrowers and the new lenders signatures.
     * TODO: Amend the [IOUTransferFlow] to handle collecting signatures from multiple parties.
     * Hint: use [initiateFlow] and the [CollectSignaturesFlow] in the same way you did for the [IOUIssueFlow].
     */
    @Test
    public void flowReturnsTransactionSignedBtAllParties() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), lender);
        Future<SignedTransaction> future = a.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
            stx.verifySignaturesExcept(mockNetwork.getDefaultNotaryIdentity().getOwningKey());
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    /**
     * Task 5.
     * We need to get the transaction signed by the notary service
     * TODO: Use a subFlow call to the [FinalityFlow] to get a signature from the lender.
     */
    @Test
    public void flowReturnsTransactionSignedByAllPartiesAndNotary() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        SignedTransaction stx = issueIOU(new IOUState(Currencies.DOLLARS(10), lender, borrower));
        IOUState inputIou = (IOUState) stx.getTx().getOutputs().get(0).getData();
        IOUTransferFlow.InitiatorFlow flow = new IOUTransferFlow.InitiatorFlow(inputIou.getLinearId(), c.getInfo().component2().get(0).getParty());
        Future<SignedTransaction> future = a.startFlow(flow);
        try {
            mockNetwork.runNetwork();
            future.get();
            stx.verifyRequiredSignatures();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }
}