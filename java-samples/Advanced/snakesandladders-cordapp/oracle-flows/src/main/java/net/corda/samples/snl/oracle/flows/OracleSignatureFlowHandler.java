package net.corda.samples.snl.oracle.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.samples.snl.service.DiceRollService;


@InitiatedBy(OracleSignatureFlow.class)
public class OracleSignatureFlowHandler extends FlowLogic<Void> {

    private FlowSession requestSession;

    public OracleSignatureFlowHandler(FlowSession requestSession) {
        this.requestSession = requestSession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        FilteredTransaction transaction = requestSession.receive(FilteredTransaction.class).unwrap(it->it);
        TransactionSignature signature = null;
        try{
            signature = getServiceHub().cordaService(DiceRollService.class).sign(transaction);
        }catch (Exception e){
            throw new FlowException(e);
        }
        requestSession.send(signature);
        return null;
    }
}
