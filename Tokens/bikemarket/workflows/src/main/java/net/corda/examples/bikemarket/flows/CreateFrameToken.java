package net.corda.examples.bikemarket.flows;

import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import net.corda.examples.bikemarket.states.FrameTokenState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class CreateFrameToken extends FlowLogic<String> {

    final private String frameModel;

    public CreateFrameToken(String frameSerial) {
        this.frameModel = frameSerial;
    }

    @Override
    public String call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
        // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        //create non-fungible frame token
        UniqueIdentifier uuid = new UniqueIdentifier();
        FrameTokenState frame = new FrameTokenState(getOurIdentity(), uuid, 0 , this.frameModel);

        //wrap it with transaction state specifying the notary
        TransactionState transactionState = new TransactionState(frame, notary);

        //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
        subFlow(new CreateEvolvableTokens(transactionState));
        return "\nCreated a frame token for bike frame. (Serial #"+ this.frameModel + ").";
    }
}
