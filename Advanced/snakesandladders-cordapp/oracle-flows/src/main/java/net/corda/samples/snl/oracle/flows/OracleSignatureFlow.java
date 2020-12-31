package net.corda.samples.snl.oracle.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;

@InitiatingFlow
public class OracleSignatureFlow extends FlowLogic<TransactionSignature> {

    private final Party oracle;
    private final FilteredTransaction ftx;

    public OracleSignatureFlow(Party oracle, FilteredTransaction ftx) {
        this.oracle = oracle;
        this.ftx = ftx;
    }

    @Suspendable
    @Override
    public TransactionSignature call() throws FlowException {
        FlowSession session = initiateFlow(oracle);
        return session.sendAndReceive(TransactionSignature.class, ftx).unwrap(it -> it);
    }
}
