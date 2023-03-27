package net.corda.samples.tokentofriend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import net.corda.samples.tokentofriend.states.CustomTokenState;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.*;

@StartableByRPC
public class QueryToken extends FlowLogic<String>{

    private String uuid;
    private String recipientEmail;

    public QueryToken(String uuid, String recipientEmail) {
        this.uuid = uuid;
        this.recipientEmail = recipientEmail;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        NonFungibleToken receivedToken = null;
        try {
            QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria().withUuid(Arrays.asList(UUID.fromString(uuid)))
                    .withStatus(Vault.StateStatus.UNCONSUMED);
            receivedToken = getServiceHub().getVaultService().queryBy(NonFungibleToken.class,inputCriteria).getStates().get(0).getState().getData();
        }catch (NoSuchElementException e){
            return "\nERROR: Your Token ID Cannot Be Found In The System";
        }
        String tokenTypeStateId = receivedToken.getToken().getTokenIdentifier();

        CustomTokenState underlineState = null;
        try {
            QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria().withUuid(Arrays.asList(UUID.fromString(tokenTypeStateId)))
                    .withStatus(Vault.StateStatus.UNCONSUMED);
            underlineState = getServiceHub().getVaultService().queryBy(CustomTokenState.class,inputCriteria).getStates().get(0).getState().getData();
        }catch(NoSuchElementException e){
            return "\nERROR: Internal Error";
        }
        if(underlineState.getReceipient().equals(this.recipientEmail)){
            return "\nCreator of the Token: " + underlineState.getIssuer() +
                    "\nMessage: " + underlineState.getMessage();
        }else{
            return "\nToken found, but the recipient email is not matched. Please try again";
        }
    }
}
