package net.corda.samples.autopayroll.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.autopayroll.contracts.MoneyStateContract;
import net.corda.samples.autopayroll.states.MoneyState;
import net.corda.samples.autopayroll.states.PaymentRequestState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class PaymentFlow {

    @InitiatingFlow
    @StartableByService
    public static class PaymentFlowInitiator extends FlowLogic<SignedTransaction> {

        @Nullable
        @Override
        public ProgressTracker getProgressTracker() {
            return super.getProgressTracker();
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Initiator flow logic goes here

            //Obtain a reference to a notary we wish to use.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            List<StateAndRef<PaymentRequestState>> wBStateList = getServiceHub().getVaultService().queryBy(PaymentRequestState.class).getStates();
            PaymentRequestState vaultState = wBStateList.get(wBStateList.size() - 1).getState().getData();
            MoneyState output = new MoneyState(Integer.parseInt(vaultState.getAmount()), vaultState.getTowhom());

            TransactionBuilder txBuilder = new TransactionBuilder(notary);
            CommandData commandData = new MoneyStateContract.Commands.Pay();
            txBuilder.addCommand(commandData, getOurIdentity().getOwningKey(), vaultState.getTowhom().getOwningKey());
            txBuilder.addOutputState(output, MoneyStateContract.ID);
            txBuilder.verify(getServiceHub());

            FlowSession session = initiateFlow(vaultState.getTowhom());
            SignedTransaction ptx = getServiceHub().signInitialTransaction(txBuilder);
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));

            return subFlow(new FinalityFlow(stx, Arrays.asList(session)));
        }
    }

    @InitiatedBy(PaymentFlow.PaymentFlowInitiator.class)
    public static class PaymentFlowResponder extends FlowLogic<Void> {
        private final FlowSession counterPartySession;

        public PaymentFlowResponder(FlowSession counterPartySession) {
            this.counterPartySession = counterPartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // responder flow logic goes here
            SignedTransaction stx = subFlow(new SignTransactionFlow(counterPartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    if (!counterPartySession.getCounterparty().equals(getServiceHub().getNetworkMapCache()
                            .getPeerByLegalName(new CordaX500Name("BankOperator", "Toronto", "CA")))) {
                        throw new FlowException("Only Bank Node can send a payment state");
                    }
                }
            });

            subFlow(new ReceiveFinalityFlow(counterPartySession, stx.getId()));
            return null;
        }
    }

}
