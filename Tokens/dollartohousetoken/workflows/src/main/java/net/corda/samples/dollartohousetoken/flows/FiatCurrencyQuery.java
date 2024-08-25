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

        FungibleToken[] receivedToken;
        String[] tokenTypeId;
        String[] amoString;
        String[] holder;
        int i = 0;
        String retString = "";

        try {
            
            VaultQueryCriteria inputQueryCriteria = new VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

            List<StateAndRef<FungibleToken>> arrList = getServiceHub().getVaultService().queryBy(FungibleToken.class,inputQueryCriteria).getStates();
            ListIterator<StateAndRef<FungibleToken>> iterator = arrList.listIterator();
            StateAndRef<FungibleToken> valueRet;

            int arraySize = 1;
            receivedToken = new FungibleToken[arraySize];
            tokenTypeId = new String[arraySize];
            amoString = new String[arraySize];
            holder = new String[arraySize];

            while (iterator.hasNext()) {
              //System.out.println("Value is : " + iterator.next());
              valueRet = iterator.next();
              System.out.println("Value :" + valueRet.getState().getData());

              if (receivedToken.length == i) {
                // expand list
                receivedToken = Arrays.copyOf(receivedToken, receivedToken.length + arraySize);
                amoString = Arrays.copyOf(amoString, amoString.length + arraySize);
                tokenTypeId = Arrays.copyOf(tokenTypeId, tokenTypeId.length + arraySize);
                holder = Arrays.copyOf(holder, holder.length + arraySize);
              }
              receivedToken[i] = valueRet.getState().getData();

              tokenTypeId[i] = receivedToken[i].getTokenType().getTokenIdentifier();
              amoString[i] = receivedToken[i].getAmount().toString();
              holder[i] = receivedToken[i].getHolder().toString();

              retString += "\n" + i + 
                ") Token type: " + tokenTypeId[i] +
                ", Amount: " + amoString[i] +
                ", Holder: " + holder[i];
              i++;
            }

            return retString;
        
        } catch (NoSuchElementException e) {
            return "\nERROR: Your Token ID Cannot Be Found In The System";
        }
    }
}
