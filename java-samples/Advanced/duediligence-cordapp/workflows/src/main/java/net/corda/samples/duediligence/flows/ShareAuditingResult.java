package net.corda.samples.duediligence.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.duediligence.contracts.CorporateRecordsContract;
import net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;

import java.util.Arrays;
import java.util.UUID;

public class ShareAuditingResult {

    @InitiatingFlow
    @StartableByRPC
    public static class ShareAuditingResultInitiator extends FlowLogic<String> {

        private UniqueIdentifier AuditingResultID;
        private Party sendTo;
        private static SecureHash trustedAuditorAttachment;


        public ShareAuditingResultInitiator(UniqueIdentifier AuditingResultID, Party sendTo, SecureHash trustedAuditorAttachment) {
            this.AuditingResultID = AuditingResultID;
            this.sendTo = sendTo;
            this.trustedAuditorAttachment = trustedAuditorAttachment;

        }

        @Override
        @Suspendable
        public String call() throws FlowException {

            //Query the input
            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(AuditingResultID.toString())))
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef inputStateAndRef = getServiceHub().getVaultService().queryBy(CorporateRecordsAuditRequest.class, inputCriteria).getStates().get(0);
            CorporateRecordsAuditRequest input = (CorporateRecordsAuditRequest) inputStateAndRef.getState().getData();

            //Send the copy to PartyB.
            SignedTransaction originalTx = getServiceHub().getValidatedTransactions().getTransaction(inputStateAndRef.getRef().getTxhash());
            //subFlow(new SendTransactionFlow(initiateFlow(sendTo), originalTx));

            //extract the notary
            Party notary = inputStateAndRef.getState().getNotary();
            //CorporateRecordsAuditRequest imageState = new CorporateRecordsAuditRequest(input.getApplicant(),input.getValidater(),input.getNumberOfFiles(),input.getLinearId(),input.getQualification());
            UniqueIdentifier copyId = new UniqueIdentifier();
            CopyOfCoporateRecordsAuditRequest copyOfResult = new CopyOfCoporateRecordsAuditRequest(input.getApplicant(),sendTo,input.getLinearId(),
                    originalTx.getId(), input.getValidater(), input.getQualification(), copyId);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addReferenceState(inputStateAndRef.referenced())
                    .addOutputState(copyOfResult)
                    .addCommand(new CorporateRecordsContract.Commands.Share(), Arrays.asList(input.getApplicant().getOwningKey(),sendTo.getOwningKey()))
                    .addAttachment(trustedAuditorAttachment);
            ;

            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(sendTo);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Notarise and record the transaction in both parties' vaults.
            subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
            return "A Copy of Corporate Auditing Report has sent to" + sendTo.getName().getOrganisation() +
                    "\nID of the Copy: "+ copyId;
        }
    }

    @InitiatedBy(ShareAuditingResultInitiator.class)
    public static class ShareAuditingResultResponder extends FlowLogic<Void> {
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public ShareAuditingResultResponder(FlowSession counterpartySession) {
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
