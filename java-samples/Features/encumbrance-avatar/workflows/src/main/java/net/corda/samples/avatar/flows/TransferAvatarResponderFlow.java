package net.corda.samples.avatar.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(TransferAvatarFlow.class)
public class TransferAvatarResponderFlow extends FlowLogic<SignedTransaction> {
    private final FlowSession counterpartySession;

    public TransferAvatarResponderFlow(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // This only gets called when we send or receive send and receive is called
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession otherPartyFlow) {
                super(otherPartyFlow);
            }
            @Override
            protected void checkTransaction(SignedTransaction stx) {
            }
        }
        final SignTxFlow signTxFlow = new SignTxFlow(counterpartySession);
        final SecureHash txId = subFlow(signTxFlow).getId();

        return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
    }
}