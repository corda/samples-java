package net.corda.examples.yo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.sun.istack.NotNull;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(YoFlow.class)
public class YoFlowResponder extends FlowLogic<SignedTransaction> {
    private final FlowSession counterpartySession;

    public YoFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });

        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}
