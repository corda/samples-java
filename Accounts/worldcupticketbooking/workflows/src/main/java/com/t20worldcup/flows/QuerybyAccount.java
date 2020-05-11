package com.t20worldcup.flows;
import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.t20worldcup.states.T20CricketTicket;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
/**
 * This will be run by the BCCI node and it will issue a nonfungible token represnting each ticket to the dealer account.
 * Buyers can then buy tickets from the dealer account.
 */
@StartableByRPC
public class QuerybyAccount extends FlowLogic<String> {
    private final String whoAmI;
    public QuerybyAccount(String whoAmI) {
        this.whoAmI = whoAmI;
    }
    @Override
    @Suspendable
    public String call() throws FlowException {
        AccountInfo myAccount = UtilitiesKt.getAccountService(this).accountInfo(whoAmI).get(0).getState().getData();
        UUID id = myAccount.getIdentifier().getId();
        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withExternalIds(Arrays.asList(id));

        //Ticket
        List<StateAndRef<NonFungibleToken>> ticketList = getServiceHub().getVaultService().queryBy(NonFungibleToken.class,criteria).getStates();
        List<String> myTicketIDs = ticketList.stream().map( it ->it.getState().getData().getTokenType().getTokenIdentifier()).collect(Collectors.toList());
        List<String> tkList = myTicketIDs.stream().map( it -> {
            UUID uuid = UUID.fromString(it);
            QueryCriteria.LinearStateQueryCriteria queryCriteria =
                    new QueryCriteria.LinearStateQueryCriteria(null,Arrays.asList(uuid),null, Vault.StateStatus.UNCONSUMED);
            StateAndRef<T20CricketTicket> stateAndRef = getServiceHub().getVaultService().queryBy(T20CricketTicket.class,queryCriteria).getStates().get(0);
            String description = stateAndRef.getState().getData().getTicketTeam();
            return description;
        }).collect(Collectors.toList());

        //Assets
        List<StateAndRef<FungibleToken>> asset = getServiceHub().getVaultService().queryBy(FungibleToken.class,criteria).getStates();
        List<String> myMoney = asset.stream().map(it -> {
            String money = "";
            money = money+it.getState().getData().getAmount().getQuantity() + " " + it.getState().getData().getTokenType().getTokenIdentifier();
            return money;
        }).collect(Collectors.toList());

        String tickets = "\nI have ticket(s) for: ";
        for(String item : tkList){
            tickets = tickets + item;
        }

        String wallets = "\nI have money of: ";
        for(String item: myMoney){
            wallets = wallets + item;
        }
        return  tickets + wallets;
    }
}