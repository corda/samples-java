package net.corda.samples.bikemarket.flows;

import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.bikemarket.states.WheelsTokenState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class CreateWheelToken extends FlowLogic<String> {

    final private String wheelsSerial;

    public CreateWheelToken(String wheelsSerial) {
        this.wheelsSerial = wheelsSerial;
    }

    @Override
    public String call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        //create non-fungible frame token
        UniqueIdentifier uuid = new UniqueIdentifier();
        WheelsTokenState wheel = new WheelsTokenState(getOurIdentity(), uuid, 0 , this.wheelsSerial);

        //wrap it with transaction state specifying the notary
        TransactionState transactionState = new TransactionState(wheel, notary);

        //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
        subFlow(new CreateEvolvableTokens(transactionState));
        return "\nCreated a wheel token for bike wheels. (Serial #"+ this.wheelsSerial + ").";
    }
}
