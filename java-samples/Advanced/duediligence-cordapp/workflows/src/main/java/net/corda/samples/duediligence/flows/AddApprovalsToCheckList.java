package net.corda.samples.duediligence.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.duediligence.contracts.DueDChecklistContract;
import net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;
import net.corda.samples.duediligence.states.DueDChecklist;

import javax.naming.ldap.UnsolicitedNotification;
import java.util.Arrays;
import java.util.UUID;

public class AddApprovalsToCheckList {

    @InitiatingFlow
    @StartableByRPC
    public static class CreateCheckListAndAddApprovalInitiator extends FlowLogic<SignedTransaction> {

        private Party reportTo;
        private UniqueIdentifier approvalId;

        public CreateCheckListAndAddApprovalInitiator(Party reportTo, UniqueIdentifier approvalId) {
            this.reportTo = reportTo;
            this.approvalId = approvalId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            //Query the input
            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(approvalId.toString())))
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef inputStateAndRef = getServiceHub().getVaultService().queryBy(ContractState.class, inputCriteria).getStates().get(0);

            //extract the notary
            Party notary = inputStateAndRef.getState().getNotary();


            //create due-diligence Checklist
            DueDChecklist checklist = new DueDChecklist(3,getOurIdentity(),reportTo,new UniqueIdentifier());
            checklist.uploadApproval(this.approvalId);

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(checklist)
                    .addCommand(new DueDChecklistContract.Commands.Add(),Arrays.asList(getOurIdentity().getOwningKey(),reportTo.getOwningKey()));

            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(reportTo);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Notarise and record the transaction in both parties' vaults.
            if (inputStateAndRef.getState().getData().getClass() == CorporateRecordsAuditRequest.class){
                CorporateRecordsAuditRequest request = (CorporateRecordsAuditRequest) inputStateAndRef.getState().getData();
                return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession,initiateFlow(request.getValidater()))));
            }else{
                CopyOfCoporateRecordsAuditRequest request = (CopyOfCoporateRecordsAuditRequest) inputStateAndRef.getState().getData();
                return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession,initiateFlow(request.getoriginalValidater()),initiateFlow(request.getOriginalOwner()))));
            }
        }
    }

    @InitiatedBy(CreateCheckListAndAddApprovalInitiator.class)
    public static class CreateCheckListAndAddApprovalResponder extends FlowLogic<Void> {
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public CreateCheckListAndAddApprovalResponder(FlowSession counterpartySession) {
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
