package net.corda.examples.notarychange.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.internal.ServiceHubCoreInternal;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.notarychange.contracts.IOUContract;
import net.corda.examples.notarychange.states.IOUState;
import net.corda.node.services.api.ServiceHubInternal;
import org.intellij.lang.annotations.Flow;

import java.util.Arrays;
import java.util.Collections;

public class SettleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier linearId;
        private Party notary;

        private final ProgressTracker.Step QUERYING_VAULT = new ProgressTracker.Step("Fetching IOU from node's vault.");
        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new IOU.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                QUERYING_VAULT,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        // Constructor
        public Initiator(UniqueIdentifier linearId) {
            this.linearId = linearId;
            this.notary = null;
        }

        // Constructor used to allow user to select notary of choice
        public Initiator(UniqueIdentifier linearId, Party notary) {
            this.linearId = linearId;
            this.notary = notary;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            progressTracker.setCurrentStep(QUERYING_VAULT);
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Collections.singletonList(linearId.getId()));
            Vault.Page results  = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            if(results.getStates().size() == 0){
                throw new FlowException("No IOU found for LinearId:" + linearId);
            }

            StateAndRef<IOUState> iouStateStateAndRef = (StateAndRef<IOUState>) results.getStates().get(0);
            IOUState inputStateToSettle = iouStateStateAndRef.getState().getData();

            if (!inputStateToSettle.getBorrower().getOwningKey().equals(getOurIdentity().getOwningKey())) {
                throw new FlowException("The borrower must initiate the flow");
            }

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party me = getOurIdentity();
            final Command<IOUContract.Commands.Settle> txCommand = new Command<>(
                    new IOUContract.Commands.Settle(),
                    Arrays.asList(inputStateToSettle.getLender().getOwningKey(), inputStateToSettle.getBorrower().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(iouStateStateAndRef)
                    .addCommand(txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(inputStateToSettle.getLender());
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Collections.singletonList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(otherPartySession)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            subFlow(new SignTransactionFlow(otherPartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    // Implement responder flow transaction checks here
                }
            });
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }
}
