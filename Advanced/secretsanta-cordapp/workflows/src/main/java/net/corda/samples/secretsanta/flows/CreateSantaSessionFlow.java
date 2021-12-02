package net.corda.samples.secretsanta.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.sun.istack.Nullable;
import net.corda.samples.secretsanta.contracts.SantaSessionContract;
import net.corda.samples.secretsanta.states.SantaSessionState;
import net.corda.core.contracts.CommandData;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;

import static java.util.Collections.singletonList;

import java.util.*;
import net.corda.core.identity.CordaX500Name;


/**
 * santaPlayers is a list of Pairs representing the names and emails of the players in this session
 */
@StartableByRPC
@InitiatingFlow
public class CreateSantaSessionFlow extends FlowLogic<SignedTransaction> {

    private List<String> playerNames;
    private List<String> playerEmails;

    private Party owner;

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
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
        Party issuer = getOurIdentity();

        SantaSessionState newSantaState = new SantaSessionState(playerNames, playerEmails, issuer, owner);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);
        CommandData commandData = new SantaSessionContract.Commands.Issue();
        transactionBuilder.addCommand(commandData, issuer.getOwningKey(), owner.getOwningKey());
        transactionBuilder.addOutputState(newSantaState, SantaSessionContract.ID);

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
