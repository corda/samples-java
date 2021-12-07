package net.corda.samples.duediligence.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.duediligence.contracts.CorporateRecordsContract;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;
import net.corda.core.identity.CordaX500Name;

import java.util.Arrays;
import java.util.List;

public class RequestToValidateCorporateRecords {

    @InitiatingFlow
    @StartableByRPC
    public static class RequestToValidateCorporateRecordsInitiator extends FlowLogic<String> {

        //private variables
        private Party validater;
        private int numberOfFiles;


        public RequestToValidateCorporateRecordsInitiator(Party validater, int numberOfFiles) {
            this.validater = validater;
            this.numberOfFiles = numberOfFiles;

        }

        @Suspendable
        @Override
        public String call() throws FlowException {
            //notary
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            //Initiate Corporate Records validation
            CorporateRecordsAuditRequest cr = new CorporateRecordsAuditRequest(getOurIdentity(),this.validater,this.numberOfFiles);

            //Build transaction
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(cr)
                    .addCommand(new CorporateRecordsContract.Commands.Propose(),
                            Arrays.asList(getOurIdentity().getOwningKey(),validater.getOwningKey()));

            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(validater);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Notarise and record the transaction in both parties' vaults.
            subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));
            return "Corporate Records Auditing Request has sent to: " + cr.getValidater().getName().getOrganisation()
                    +"\nCase Id: "+ cr.getLinearId();
        }
    }

    @InitiatedBy(RequestToValidateCorporateRecordsInitiator.class)
    public static class RequestToValidateCorporateRecordsResponder extends FlowLogic<Void> {
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public RequestToValidateCorporateRecordsResponder(FlowSession counterpartySession) {
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
