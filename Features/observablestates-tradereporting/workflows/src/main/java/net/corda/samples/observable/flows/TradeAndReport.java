package  net.corda.samples.observable.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.observable.contracts.HighlyRegulatedContract;
import net.corda.samples.observable.states.HighlyRegulatedState;

import java.util.Arrays;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class TradeAndReport extends FlowLogic<Void> {

    private final Party buyer;
    private final Party stateRegulator;
    private final Party nationalRegulator;

    public TradeAndReport(Party buyer, Party stateRegulator, Party nationalRegulator) {
        this.buyer = buyer;
        this.stateRegulator = stateRegulator;
        this.nationalRegulator = nationalRegulator;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        HighlyRegulatedState outputState = new HighlyRegulatedState(buyer, getOurIdentity());

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, HighlyRegulatedContract.ID)
                .addCommand(new HighlyRegulatedContract.Commands.Trade(), getOurIdentity().getOwningKey());

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        List<FlowSession> sessions = Arrays.asList(initiateFlow(buyer), initiateFlow(stateRegulator));
        // We distribute the transaction to both the buyer and the state regulator using `FinalityFlow`.

        subFlow(new FinalityFlow(signedTransaction, sessions));

        // We also distribute the transaction to the national regulator manually.
        subFlow(new ReportManually(signedTransaction, nationalRegulator));

        return null;
    }
}
