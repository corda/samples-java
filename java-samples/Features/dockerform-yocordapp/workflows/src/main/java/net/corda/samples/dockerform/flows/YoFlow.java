package net.corda.samples.dockerform.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.dockerform.contracts.YoContract;
import net.corda.samples.dockerform.states.YoState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
public class YoFlow extends FlowLogic<SignedTransaction> {
    private static final ProgressTracker.Step CREATING = new ProgressTracker.Step("Creating a new Yo!");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the Yo!");
    private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verfiying the Yo!");
    private static final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Sending the Yo!") {
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
            FINALISING
    );

    @Nullable
    @Override
    public ProgressTracker getProgressTracker() {
        return this.progressTracker;
    }

    private final Party target;

    public YoFlow(Party target) {
        this.target = target;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        this.progressTracker.setCurrentStep(CREATING);

        Party me = getOurIdentity();
        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        Command<YoContract.Commands.Send> command = new Command<YoContract.Commands.Send>(new YoContract.Commands.Send(), Arrays.asList(me.getOwningKey()));
        YoState state = new YoState(me, this.target);
        StateAndContract stateAndContract = new StateAndContract(state, YoContract.ID);
        TransactionBuilder utx = new TransactionBuilder(notary).withItems(stateAndContract, command);

        this.progressTracker.setCurrentStep(VERIFYING);
        utx.verify(getServiceHub());

        this.progressTracker.setCurrentStep(SIGNING);
        SignedTransaction stx = getServiceHub().signInitialTransaction(utx);

        this.progressTracker.setCurrentStep(FINALISING);
        FlowSession targetSession = initiateFlow(this.target);
        return subFlow(new FinalityFlow(stx, Collections.singletonList(targetSession), Objects.requireNonNull(FINALISING.childProgressTracker())));
    }
}
