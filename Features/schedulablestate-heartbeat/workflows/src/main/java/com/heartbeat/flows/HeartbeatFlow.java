package com.heartbeat.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.heartbeat.contracts.HeartContract;
import com.heartbeat.contracts.HeartState;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;

import java.util.Collections;

/**
 * This is the flow that a Heartbeat state runs when it consumes itself to create a new Heartbeat
 * state on the ledger.
 */
@InitiatingFlow
@SchedulableFlow
public class HeartbeatFlow extends FlowLogic<String> {
    private final StateRef stateRef;
    private final ProgressTracker progressTracker = tracker();

    private static final Step GENERATING_TRANSACTION = new Step("Generating a HeartState transaction");
    private static final Step SIGNING_TRANSACTION = new Step("Signing transaction with out private key.");
    private static final Step FINALISING_TRANSACTION = new Step("Recording transaction") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.tracker();
        }
    };

    private ProgressTracker tracker() {
        return new ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );
    }

    public HeartbeatFlow(StateRef stateRef) {
        this.stateRef = stateRef;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        StateAndRef<HeartState> input = getServiceHub().toStateAndRef(stateRef);
        HeartState output = new HeartState(getOurIdentity());
        CommandData beatCmd = new HeartContract.Commands.Beat();

        // Obtain a reference to a notary we wish to use.
        /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
        // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        TransactionBuilder txBuilder = new TransactionBuilder(notary)
            .addInputState(input)
            .addOutputState(output, HeartContract.contractID)
            .addCommand(beatCmd, getOurIdentity().getOwningKey());

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        subFlow(new FinalityFlow(signedTx, Collections.emptyList()));

        return "Lub-dub";
    }
}
