package net.corda.samples.duediligence.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.duediligence.contracts.CorporateRecordsContract;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;

import java.util.Arrays;
import java.util.UUID;

public class ValidateCorporateRecords {

    @InitiatingFlow
    @StartableByRPC
    public static class ValidateCorporateRecordsInitiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier linearId;


        public ValidateCorporateRecordsInitiator(UniqueIdentifier linearId) {
            this.linearId = linearId;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            //Query the input
            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(linearId.toString())))
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef inputStateAndRef = getServiceHub().getVaultService().queryBy(CorporateRecordsAuditRequest.class, inputCriteria).getStates().get(0);
            CorporateRecordsAuditRequest input = (CorporateRecordsAuditRequest) inputStateAndRef.getState().getData();

            //extract the notary
            Party notary = inputStateAndRef.getState().getNotary();

            //Creating the output
            CorporateRecordsAuditRequest output = new CorporateRecordsAuditRequest(input.getApplicant(),getOurIdentity(),input.getNumberOfFiles(),input.getLinearId());

            //set validation status to true
            output.validatedAndApproved();

            //Build transaction
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(output)
                    .addCommand(new CorporateRecordsContract.Commands.Validate(),
                            Arrays.asList(getOurIdentity().getOwningKey(),input.getApplicant().getOwningKey()));

            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(input.getApplicant());
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
        }
    }

    @InitiatedBy(ValidateCorporateRecordsInitiator.class)
    public static class ValidateCorporateRecordsResponder extends FlowLogic<Void> {
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public ValidateCorporateRecordsResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                }
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }
}
