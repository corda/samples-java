package net.corda.samples.logging.flows;

import co.paralleluniverse.fibers.Suspendable;
//import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.logging.contracts.YoContract;
import net.corda.samples.logging.states.YoState;
import org.jetbrains.annotations.Nullable;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
public class YoFlow extends FlowLogic<SignedTransaction> {
    private static final ProgressTracker.Step CREATING = new ProgressTracker.Step("Creating a new Yo!");
    private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing the Yo!");
    private static final ProgressTracker.Step VERIFYING = new ProgressTracker.Step("Verifying the Yo!");
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
        return progressTracker;
    }

    private final Party target;

    public YoFlow(Party target) {
        this.target = target;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // note we're creating a logger first with the shared name from our other example.
        Logger logger = LoggerFactory.getLogger("net.corda");

        progressTracker.setCurrentStep(CREATING);

        Party me = getOurIdentity();

        // here we have our first opportunity to log out the contents of the flow arguments.
        ThreadContext.put("initiator", me.getName().toString());
        ThreadContext.put("target", target.getName().toString());
        // publish to the log with the additional context
        logger.info("Initializing the transaction.");
        // flush the threadContext
        ThreadContext.removeAll(Arrays.asList("initiator", "target"));

        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        Command<YoContract.Commands.Send> command = new Command<YoContract.Commands.Send>(new YoContract.Commands.Send(), Arrays.asList(me.getOwningKey()));
        YoState state = new YoState(me, target);
        StateAndContract stateAndContract = new StateAndContract(state, YoContract.ID);
        TransactionBuilder utx = new TransactionBuilder(notary).withItems(stateAndContract, command);

        progressTracker.setCurrentStep(VERIFYING);
        utx.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING);
        SignedTransaction stx = getServiceHub().signInitialTransaction(utx);

        // inject details to the threadcontext to be exported as json
        ThreadContext.put("tx_id", stx.getId().toString());
        ThreadContext.put("notary", notary.getName().toString());
        // publish to the log with the additional context
        logger.info("Finalizing the transaction.");
        // flush the threadContext
        ThreadContext.removeAll(Arrays.asList("tx_id", "notary"));

        progressTracker.setCurrentStep(FINALISING);
        FlowSession targetSession = initiateFlow(target);
        return subFlow(new FinalityFlow(stx, Arrays.asList(targetSession), Objects.requireNonNull(FINALISING.childProgressTracker())));
    }
}
