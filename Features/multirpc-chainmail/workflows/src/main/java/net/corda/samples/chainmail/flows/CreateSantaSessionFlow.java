package net.corda.samples.chainmail.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.sun.istack.Nullable;
import net.corda.samples.chainmail.contracts.ChainMailSessionContract;
import net.corda.samples.chainmail.states.ChainMailSessionState;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;

import static java.util.Collections.singletonList;

import java.util.*;


@StartableByRPC
@InitiatingFlow
public class CreateSantaSessionFlow extends FlowLogic<SignedTransaction> {

    private final List<String> playerNames;
    private final List<String> playerEmails;

    private final Party owner;

    // elves 'own' a secret santa session
    public CreateSantaSessionFlow(List<String> playerNames, List<String> playerEmails, Party owner) {
        this.playerNames = playerNames;
        this.playerEmails = playerEmails;
        this.owner = owner;
    }

    private static final ProgressTracker.Step CREATING = new ProgressTracker.Step("Shoveling coal in the server . . .");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Getting the message to Santa . . .");
    private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Gathering the reindeer . . .");
    private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Sending christmas cheer!");
    private static final ProgressTracker.Step FINALDISPLAY = new ProgressTracker.Step("Secret Santa has been successfully generated.") {
        @Nullable
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    ProgressTracker progressTracker = new ProgressTracker(
            CREATING,
            SIGNING,
            VERIFYING,
            FINALISING,
            FINALDISPLAY
    );

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // run an issuance transaction for a new secret santa game
        progressTracker.setCurrentStep(CREATING);
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party issuer = getOurIdentity();

        ChainMailSessionState newSantaState = new ChainMailSessionState(playerNames, playerEmails, issuer, owner);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        CommandData commandData = new ChainMailSessionContract.Commands.Issue();
        transactionBuilder.addCommand(commandData, issuer.getOwningKey(), owner.getOwningKey());
        transactionBuilder.addOutputState(newSantaState, ChainMailSessionContract.ID);

        progressTracker.setCurrentStep(VERIFYING);
        transactionBuilder.verify(getServiceHub());
        FlowSession session = initiateFlow(owner);

        progressTracker.setCurrentStep(SIGNING);
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        progressTracker.setCurrentStep(FINALISING);
        SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

        progressTracker.setCurrentStep(FINALDISPLAY);
        return subFlow(new FinalityFlow(fullySignedTransaction, singletonList(session)));
    }

}
