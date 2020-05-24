package com.pr.consultant.responder;

import co.paralleluniverse.fibers.Suspendable;
import com.pr.common.flow.PRFlow;
import com.pr.contract.state.schema.states.PRState;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

// ******************
// * Responder flow *
// ******************
@InitiatedBy(PRFlow.class)
public class ConsultantResponder extends FlowLogic<SignedTransaction> {
    private FlowSession counterpartySession;

    public ConsultantResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Responder flow logic goes here.

        class SignTxFlow extends SignTransactionFlow {


            public SignTxFlow(@NotNull FlowSession otherSideSession, @NotNull ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                requireThat(require -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    require.using("This must be the PR State.", output instanceof PRState);
                    return null;
                });

            }


        }

        final SignTxFlow signTxFlow = new SignTxFlow(counterpartySession,SignTransactionFlow.Companion.tracker());
        final SecureHash txId = subFlow(signTxFlow).getId();

        return subFlow(new ReceiveFinalityFlow(counterpartySession,txId));
    }
}
