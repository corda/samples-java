package net.corda.samples.flow;


import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.*;
import net.corda.testing.node.*;
import net.corda.core.identity.Party;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.TransactionBuilder;


import net.corda.samples.contracts.IOUContract;
import net.corda.samples.flows.IOUIssueFlow;
import net.corda.samples.states.IOUState;

import java.util.stream.Collectors;
import java.util.concurrent.Future;
import java.util.*;

import org.junit.*;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.hamcrest.core.IsInstanceOf.*;

/**
 * Practical exercise instructions Flows part 1.
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
public class IOUIssueFlowTests {

    private MockNetwork mockNetwork;
    private StartedMockNode a, b;

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

        ArrayList<StartedMockNode> startedNodes = new ArrayList<>();
        startedNodes.add(a);
        startedNodes.add(b);

        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach(el -> el.registerInitiatedFlow(IOUIssueFlow.ResponderFlow.class));
        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Task 1.
     * Build out the {@link IOUIssueFlow}!
     * TODO: Implement the {@link IOUIssueFlow} flow which builds and returns a partially {@link SignedTransaction}.
     * Hint:
     * - There's a lot to do to get this unit test to pass!
     * - Create a {@link TransactionBuilder} and pass it a notary reference.
     * -- A notary {@link Party} object can be obtained from [FlowLogic.getServiceHub().getNetworkMapCache().getNotaryIdentities()].
     * -- In this training project there is only one notary
     * - Create a new {@link Command} object with the [IOUContract.Commands.Issue] type
     * -- The required signers will be the same as the state's participants
     * -- Add the {@link Command} to the transaction builder [addCommand].
     * - Use the flow's {@link IOUState} parameter as the output state with [addOutputState]
     * - Extra credit: use [TransactionBuilder.withItems] to create the transaction instead
     * - Sign the transaction and convert it to a {@link SignedTransaction} using the [getServiceHub().signInitialTransaction] method.
     * - Return the {@link SignedTransaction}.
     */
    @Test
    public void flowReturnsCorrectlyFormedPartiallySignedTransaction() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();

        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iou);

        Future<SignedTransaction> future = a.startFlow(flow);
        mockNetwork.runNetwork();

        // Return the unsigned(!) SignedTransaction object from the IOUIssueFlow.
        SignedTransaction ptx = future.get();

        // Print the transaction for debugging purposes.
        System.out.println(ptx.getTx());

        // Check the transaction is well formed...
        // No outputs, one input IOUState and a command with the right properties.
        assert (ptx.getTx().getInputs().isEmpty());
        assert (ptx.getTx().getOutputs().get(0).getData() instanceof IOUState);

        Command command = ptx.getTx().getCommands().get(0);
        assert (command.getValue() instanceof IOUContract.Commands.Issue);
        assert (new HashSet<>(command.getSigners()).equals(
                new HashSet<>(iou.getParticipants()
                        .stream().map(el -> el.getOwningKey())
                        .collect(Collectors.toList()))));

        ptx.verifySignaturesExcept(borrower.getOwningKey(),
                mockNetwork.getDefaultNotaryNode().getInfo().getLegalIdentitiesAndCerts().get(0).getOwningKey());
    }

    /**
     * Task 2.
     * Now we have a well formed transaction, we need to properly verify it using the {@link IOUContract}.
     * TODO: Amend the {@link IOUIssueFlow} to verify the transaction as well as sign it.
     * Hint: You can verify on the builder directly prior to finalizing the transaction. This way
     * you can confirm the transaction prior to making it immutable with the signature.
     */
    @Test
    public void flowReturnsVerifiedPartiallySignedTransaction() throws Exception {
        // Check that a zero amount IOU fails.
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();

        IOUState zeroIou = new IOUState(Currencies.POUNDS(0), lender, borrower);
        Future<SignedTransaction> futureOne = a.startFlow(new IOUIssueFlow.InitiatorFlow(zeroIou));
        mockNetwork.runNetwork();

        exception.expectCause(instanceOf(TransactionVerificationException.class));

        futureOne.get();

        // Check that an IOU with the same participants fails.
        IOUState borrowerIsLenderIou = new IOUState(Currencies.POUNDS(10), lender, lender);
        Future<SignedTransaction> futureTwo = a.startFlow(new IOUIssueFlow.InitiatorFlow(borrowerIsLenderIou));
        mockNetwork.runNetwork();
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        futureTwo.get();

        // Check a good IOU passes.
        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        Future<SignedTransaction> futureThree = a.startFlow(new IOUIssueFlow.InitiatorFlow(iou));
        mockNetwork.runNetwork();
        futureThree.get();
    }

    /**
     * IMPORTANT: Review the {@link CollectSignaturesFlow} before continuing here.
     * Task 3.
     * Now we need to collect the signature from the [otherParty] using the {@link CollectSignaturesFlow}.
     * TODO: Amend the {@link IOUIssueFlow} to collect the [otherParty]'s signature.
     * Hint:
     * On the Initiator side:
     * - Get a set of the required signers from the participants who are not the node - refer to Task 6 of IOUIssueTests
     * - - [getOurIdentity()] will give you the identity of the node you are operating as
     * - Use [initateFlow] to get a set of {@link FlowSession} objects
     * - - Using [state.participants] as a base to determine the sessions needed is recommended. [participants] is on
     * - - the state interface so it is guaranteed to to exist where [lender] and [borrower] are not.
     * - Use [subFlow] to start the {@link CollectSignaturesFlow}
     * - Pass it a {@link SignedTransaction} object and {@link FlowSession} set
     * - It will return a {@link SignedTransaction} with all the required signatures
     * - The subflow performs the signature checking and transaction verification for you
     * <p>
     * On the Responder side:
     * - Create a subclass of {@link SignTransactionFlow}
     * - Override [SignTransactionFlow.checkTransaction] to impose any constraints on the transaction
     * <p>
     * Using this flow you abstract away all the back-and-forth communication required for parties to sign a
     * transaction.
     */
    @Test
    public void flowReturnsTransactionSignedByBothParties() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iou);

        Future<SignedTransaction> future = a.startFlow(flow);
        mockNetwork.runNetwork();

        SignedTransaction stx = future.get();
        stx.verifyRequiredSignatures();
    }

    /**
     * Task 4.
     * Now we need to store the finished {@link SignedTransaction} in both counter-party vaults.
     * TODO: Amend the {@link IOUIssueFlow} by adding a call to {@link FinalityFlow}.
     * Hint:
     * - As mentioned above, use the {@link FinalityFlow} to ensure the transaction is recorded in both {@link Party} vaults.
     * - Do not use the [BroadcastTransactionFlow]!
     * - The {@link FinalityFlow} determines if the transaction requires notarisation or not.
     * - We don't need the notary's signature as this is an issuance transaction without a timestamp. There are no
     * inputs in the transaction that could be double spent! If we added a timestamp to this transaction then we
     * would require the notary's signature as notaries act as a timestamping authority.
     */
    @Test
    public void flowRecordsTheSameTransactionInBothPartyVaults() throws Exception {
        Party lender = a.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        Party borrower = b.getInfo().getLegalIdentitiesAndCerts().get(0).getParty();
        IOUState iou = new IOUState(Currencies.POUNDS(10), lender, borrower);
        IOUIssueFlow.InitiatorFlow flow = new IOUIssueFlow.InitiatorFlow(iou);

        Future<SignedTransaction> future = a.startFlow(flow);
        mockNetwork.runNetwork();
        SignedTransaction stx = future.get();
        System.out.printf("Signed transaction hash: %h\n", stx.getId());

        Arrays.asList(a, b).stream().map(el ->
                el.getServices().getValidatedTransactions().getTransaction(stx.getId())
        ).forEach(el -> {
            SecureHash txHash = el.getId();
            System.out.printf("$txHash == %h\n", stx.getId());
            assertEquals(stx.getId(), txHash);
        });
    }
}