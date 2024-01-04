package net.corda.samples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.identity.Party;
import net.corda.core.contracts.*;
import net.corda.core.crypto.CryptoUtils;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.ServicesForResolution;
import net.corda.core.node.services.AttachmentStorage;
import net.corda.core.node.services.IdentityService;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.AttachmentQueryCriteria.AttachmentsQueryCriteria;
import net.corda.core.node.services.vault.*;
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;

import java.util.*;

@StartableByRPC
public class FiatCurrencyQuery extends FlowLogic<String>{
    private final String currency;
    private final Party recipient;

    public FiatCurrencyQuery(String currency, Party recipient) {
        this.currency = currency;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        FungibleToken receivedToken = null;
        try {
            
            VaultQueryCriteria inputQueryCriteria = new VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

            receivedToken = getServiceHub().getVaultService().queryBy(FungibleToken.class,inputQueryCriteria).getStates().get(0).getState().getData();
        }catch (NoSuchElementException e){
            return "\nERROR: Your Token ID Cannot Be Found In The System";
        }
        String tokenTypeId = receivedToken.getTokenType().getTokenIdentifier();
        String amoString = receivedToken.getAmount().toString();

        {
            return "\nthe Token type: " + tokenTypeId +
                   "\nAmount: " + amoString;
        }
    }
}
