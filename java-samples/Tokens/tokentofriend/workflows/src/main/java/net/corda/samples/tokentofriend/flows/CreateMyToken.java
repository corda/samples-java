package net.corda.samples.tokentofriend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.tokentofriend.states.CustomTokenState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class CreateMyToken extends FlowLogic<UniqueIdentifier>{

    private String myEmail;
    private String recipients;
    private String msg;

    public CreateMyToken(String myEmail, String recipients, String msg) {
        this.myEmail = myEmail;
        this.recipients = recipients;
        this.msg = msg;
    }


    @Override
    @Suspendable
    public UniqueIdentifier call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

        UniqueIdentifier uuid = new UniqueIdentifier();
        CustomTokenState tokenState = new CustomTokenState(myEmail,recipients,msg,getOurIdentity(),0,uuid);

        //warp it with transaction state specifying the notary
        TransactionState<CustomTokenState> transactionState = new TransactionState<>(tokenState,notary);

        subFlow(new CreateEvolvableTokens(transactionState));
        return uuid;
    }
}
