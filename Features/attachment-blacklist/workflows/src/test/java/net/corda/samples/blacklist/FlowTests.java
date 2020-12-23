package net.corda.samples.blacklist;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Attachment;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.blacklist.states.AgreementState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private Party aIdentity;
    private Party bIdentity;
    private String agreementTxt;
    private SecureHash blacklistAttachment;
    private SecureHash incorrectAttachment;

    @Before
    public void setup() throws FileNotFoundException {
        this.network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.blacklist.contracts"))));
        this.a = this.network.createNode();
        this.b = this.network.createNode();
        this.aIdentity = this.a.getInfo().getLegalIdentities().get(0);
        this.bIdentity = this.b.getInfo().getLegalIdentities().get(0);

        this.agreementTxt = this.aIdentity.getName() + " agrees with " + this.bIdentity.getName() + " that...";

        // We upload the valid attachment to the first node, who will propagate it to the other node as part of the
        // flow.
        FileInputStream attachmentInputStream = new FileInputStream(new File(Constants.BLACKLIST_JAR_PATH));

        this.a.transaction(() -> {
            try {
                this.blacklistAttachment = this.a.getServices().getAttachments().importAttachment(attachmentInputStream, "user", "blacklist");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });

        // We upload the invalid attachment to the first node, who will propagate it to the other node as part of the
        // flow.
        FileInputStream incorrectAttachmentInputStream = new FileInputStream(new File(Constants.INCORRECT_JAR_PATH));

        this.a.transaction(() -> {
            try {
                this.incorrectAttachment = this.a.getServices().getAttachments().importAttachment(incorrectAttachmentInputStream, "user", "blacklist");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });

        this.b.registerInitiatedFlow(AgreeFlow.class);
        this.network.runNetwork();
    }

    @After
    public void tearDown() {
        this.network.stopNodes();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void flowRejectsAttachmentsThatDoNotMeetTheConstraintsOfAttachmentContract() throws ExecutionException, InterruptedException {
        this.thrown.expectCause(IsInstanceOf.<Throwable>instanceOf(TransactionVerificationException.class));

        // The attachment being passed to the propose flow is INVALID, will be rejected.
        ProposeFlow flow = new ProposeFlow(this.agreementTxt, this.incorrectAttachment, this.bIdentity);
        CordaFuture future = this.a.startFlow(flow);
        this.network.runNetwork();
        future.get();
    }

    @Test
    public void flowRecordsTheCorrectTransactionInBothPartiesTransactionStorages() throws ExecutionException, InterruptedException {
        this.reachAgreement();

        // We check the recorded agreement in both vaults.
        for (StartedMockNode node : ImmutableList.of(this.a, this.b)) {
            node.transaction(() -> {
                List<StateAndRef<AgreementState>> agreements = node.getServices().getVaultService().queryBy(AgreementState.class).getStates();
                assertEquals(1, agreements.size());

                AgreementState recordedState = agreements.get(0).getState().getData();
                assertEquals(this.aIdentity, recordedState.getPartyA());
                assertEquals(this.bIdentity, recordedState.getPartyB());
                assertEquals(this.agreementTxt, recordedState.getTxt());

                return null;
            });
        }
    }

    @Test
    public void flowPropagatesTheAttachmentToBsAttachmentStorage() throws ExecutionException, InterruptedException {
        this.reachAgreement();

        this.b.transaction(() -> {
            Attachment blacklist = this.b.getServices().getAttachments().openAttachment(this.blacklistAttachment);
            assertNotNull(blacklist);

            return null;
        });
    }

    private SignedTransaction reachAgreement() throws ExecutionException, InterruptedException {
        ProposeFlow flow = new ProposeFlow(this.agreementTxt, this.blacklistAttachment, this.bIdentity);
        CordaFuture future = this.a.startFlow(flow);
        this.network.runNetwork();
        return (SignedTransaction) future.get();
    }
}
