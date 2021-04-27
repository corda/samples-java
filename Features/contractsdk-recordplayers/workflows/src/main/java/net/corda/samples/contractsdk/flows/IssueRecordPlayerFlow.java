package net.corda.samples.contractsdk.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.contractsdk.contracts.RecordPlayerContract;
import net.corda.samples.contractsdk.states.Needle;
import net.corda.samples.contractsdk.states.RecordPlayerState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class IssueRecordPlayerFlow extends FlowLogic<SignedTransaction> {

    // We will not use these ProgressTracker for this Hello-World sample
    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    private Party dealer;
    private Party manufacturer;
    private String needle;

    /*
     * A new record player is issued only from the manufacturer to an exclusive dealer.
     * Most of the settings are default
     */
    public IssueRecordPlayerFlow(Party dealer, String needle) {
        this.dealer = dealer;
        this.needle = needle;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        // ideally this is only run by the manufacturer
        this.manufacturer = getOurIdentity();

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        Needle n = Needle.SPHERICAL;

        if (needle.equals("elliptical")) {
            n = Needle.ELLIPTICAL;
        }

        RecordPlayerState output = new RecordPlayerState(this.manufacturer, this.dealer, n, 100, 700, 10000, 0, new UniqueIdentifier());

        // Create a new TransactionBuilder object.
        final TransactionBuilder builder = new TransactionBuilder(notary);

        // Add the iou as an output state, as well as a command to the transaction builder.
        builder.addOutputState(output);
        builder.addCommand(new RecordPlayerContract.Commands.Issue(), Arrays.asList(this.manufacturer.getOwningKey(), this.dealer.getOwningKey()));

        // verify and sign it with our KeyPair.
        builder.verify(getServiceHub());
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Collect the other party's signature using the SignTransactionFlow.

        List<Party> otherParties = output.getParticipants().stream().map(el -> (Party) el).collect(Collectors.toList());
        otherParties.remove(getOurIdentity());

//        FlowSession targetSession = initiateFlow(this.dealer);
//        return subFlow(new FinalityFlow(ptx, Collections.singletonList(targetSession)));


        List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());

        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

        // Assuming no exceptions, we can now finalise the transaction
        return subFlow(new FinalityFlow(stx, sessions));
    }
}



