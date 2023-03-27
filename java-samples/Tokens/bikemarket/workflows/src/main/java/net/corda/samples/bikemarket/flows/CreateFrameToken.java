package net.corda.samples.bikemarket.flows;

import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.bikemarket.states.FrameTokenState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class CreateFrameToken extends FlowLogic<String> {

    final private String frameSerial;

    public CreateFrameToken(String frameSerial) {
        this.frameSerial = frameSerial;
    }

    @Override
    public String call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        //create non-fungible frame token
        UniqueIdentifier uuid = new UniqueIdentifier();
        FrameTokenState frame = new FrameTokenState(getOurIdentity(), uuid, 0 , this.frameSerial);

        //wrap it with transaction state specifying the notary
        TransactionState transactionState = new TransactionState(frame, notary);

        //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
        subFlow(new CreateEvolvableTokens(transactionState));
        return "\nCreated a frame token for bike frame. (Serial #"+ this.frameSerial + ").";
    }
}
