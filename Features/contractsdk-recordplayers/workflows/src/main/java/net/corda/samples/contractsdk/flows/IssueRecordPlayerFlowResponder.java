package net.corda.samples.contractsdk.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(IssueRecordPlayerFlow.class)
public class IssueRecordPlayerFlowResponder extends FlowLogic<Void> {

    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public IssueRecordPlayerFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
            @Suspendable
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // we could include additional checks here but it's not necessary for the purposes of the contract sdk
            }
        });

        //Stored the transaction into data base.
        subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
        return null;
    }
}
