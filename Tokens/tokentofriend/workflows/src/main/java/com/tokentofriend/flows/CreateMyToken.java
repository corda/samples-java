package com.tokentofriend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import com.tokentofriend.states.CustomTokenState;
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

        // Obtain a reference from a notary we wish to use.
        /**
         *  METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        // val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

        UniqueIdentifier uuid = new UniqueIdentifier();
        CustomTokenState tokenState = new CustomTokenState(myEmail,recipients,msg,getOurIdentity(),0,uuid);

        //warp it with transaction state specifying the notary
        TransactionState<CustomTokenState> transactionState = new TransactionState<>(tokenState,notary);

        subFlow(new CreateEvolvableTokens(transactionState));
        return uuid;
    }
}
