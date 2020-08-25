package com.t20worldcup.flows;


import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import com.t20worldcup.states.T20CricketTicket;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * This is the DVP flow, where the buyer account buys the ticket token from the dealer account and in turn transfers him cash worth of the ticket.
 * Once buyer1 buys the token from the dealer, he can further sell this ticket to other buyers.
 * Note : this flow handles dvp from account to account on same node. This flow later will be modified if a buyer on dealer1 node wants to buy ticket from
 * dealer2 node.
 */
@StartableByRPC
@InitiatingFlow
public class DVPAccountsOnSameNode extends FlowLogic<String> {

    private final String tokenId;
    private final String buyerAccountName;
    private final String sellerAccountName;
    private final String currency;
    private final Long costOfTicket;

    public DVPAccountsOnSameNode(String tokenId, String buyerAccountName, String sellerAccountName, Long costOfTicket, String currency) {
        this.tokenId = tokenId;
        this.buyerAccountName = buyerAccountName;
        this.sellerAccountName = sellerAccountName;
        this.costOfTicket = costOfTicket;
        this.currency = currency;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        //Get buyers and sellers account infos
        AccountInfo buyerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(buyerAccountName).get(0).getState().getData();
        AccountInfo sellerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(sellerAccountName).get(0).getState().getData();

        //Generate new keys for buyers and sellers
        AnonymousParty buyerAccount = subFlow(new RequestKeyForAccount(buyerAccountInfo));
        AnonymousParty sellerAccount = subFlow(new RequestKeyForAccount(sellerAccountInfo));


        //Part1 : Move non fungible token - ticket from seller to buyer
        ///All of the Tickets Seller has
        QueryCriteria queryCriteriaForSellerTicketType = new QueryCriteria.VaultQueryCriteria()
                .withExternalIds(Arrays.asList(sellerAccountInfo.getIdentifier().getId()))
                .withStatus(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<NonFungibleToken>> allNonfungibleTokens = getServiceHub().getVaultService()
                .queryBy(NonFungibleToken.class, queryCriteriaForSellerTicketType).getStates();

        //Retrieve the one that he wants to sell
        StateAndRef<NonFungibleToken> matchedNonFungibleToken;

        if(allNonfungibleTokens.stream()
                .filter(i-> i.getState().getData().getTokenType().getTokenIdentifier().equals(tokenId))
                .findAny().isPresent()) {
            matchedNonFungibleToken = allNonfungibleTokens.stream()
                    .filter(i-> i.getState().getData().getTokenType().getTokenIdentifier().equals(tokenId))
                    .findAny().get();
        }
        else
            throw new FlowException(sellerAccountName + "does not own ticket with token id - " + tokenId);

        String ticketId = matchedNonFungibleToken.getState().getData().getTokenType().getTokenIdentifier();

        //construct the query criteria and get the base token type
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria().withUuid(Arrays.asList(UUID.fromString(ticketId))).
                withStatus(Vault.StateStatus.UNCONSUMED);

        // grab the ticket off the ledger
        StateAndRef<T20CricketTicket> stateAndRef = getServiceHub().getVaultService().
                queryBy(T20CricketTicket.class, queryCriteria).getStates().get(0);

        T20CricketTicket evolvableTokenType = stateAndRef.getState().getData();

        //get the pointer pointer to the T20CricketTicket
        TokenPointer tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.getClass());

        // Obtain a reference to a notary we wish to use.
        /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
         *
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
        // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        //create a transactionBuilder
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        //first part of DVP is to transfer the non fungible token from seller to buyer
        //this add inputs and outputs to transactionBuilder
        MoveTokensUtilitiesKt.addMoveNonFungibleTokens(transactionBuilder, getServiceHub(), tokenPointer, buyerAccount);

        //Part2 : Move fungible token - cash from buyer to seller

        QueryCriteria queryCriteriaForTokenBalance = QueryUtilitiesKt.heldTokenAmountCriteria(this.getInstance(currency), buyerAccount).and(QueryUtilitiesKt.sumTokenCriteria());

        List<Object> sum = getServiceHub().getVaultService().
                queryBy(FungibleToken.class, queryCriteriaForTokenBalance).component5();

        if(sum.size() == 0)
            throw new FlowException(buyerAccountName + "has 0 token balance. Please ask the Bank to issue some cash.");
        else {
            Long tokenBalance = (Long) sum.get(0);
            if(tokenBalance < costOfTicket)
                throw new FlowException("Available token balance of " + buyerAccountName+ " is less than the cost of the ticket. Please ask the Bank to issue some cash if you wish to buy the ticket ");
        }

        Amount<FiatCurrency> amount = new Amount(costOfTicket, FiatCurrency.Companion.getInstance(currency));

        //move money to sellerAccountInfo account.
        PartyAndAmount partyAndAmount = new PartyAndAmount(sellerAccount, amount);

        //construct the query criteria and get all available unconsumed fungible tokens which belong to buyers account
        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED).
                withExternalIds(Arrays.asList(buyerAccountInfo.getIdentifier().getId()));

        //call utility function to move the fungible token from buyer to seller account
        //this also adds inputs and outputs to the transactionBuilder
        //till now we have only 1 transaction with 2 inputs and 2 outputs - one moving fungible tokens other moving non fungible tokens between accounts
        MoveTokensUtilitiesKt.addMoveFungibleTokens(transactionBuilder, getServiceHub(), Arrays.asList(partyAndAmount), buyerAccount, criteria);

        //self sign the transaction. note : the host party will first self sign the transaction.
        SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder,
                Arrays.asList(getOurIdentity().getOwningKey()));

        //establish sessions with buyer and seller. to establish session get the host name from accountinfo object
        FlowSession customerSession = initiateFlow(buyerAccountInfo.getHost());

        FlowSession dealerSession = initiateFlow(sellerAccountInfo.getHost());

        //Note: though buyer and seller are on the same node still we will have to call CollectSignaturesFlow as the signer is not a Party but an account.
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(selfSignedTransaction,
                Arrays.asList(customerSession, dealerSession)));

        //call ObserverAwareFinalityFlow for finality
        SignedTransaction stx = subFlow(new ObserverAwareFinalityFlow(fullySignedTx, Arrays.asList(customerSession, dealerSession)));

        return ("The ticket is sold to "+buyerAccountName+""+ "\ntxID: " + stx.getId().toString());
    }

    public TokenType getInstance(String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        return new TokenType(currency.getCurrencyCode(), 0);
    }
}

@InitiatedBy(DVPAccountsOnSameNode.class)
class DVPAccountsOnSameNodeResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public DVPAccountsOnSameNodeResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        subFlow(new SignTransactionFlow(otherSide) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });

        subFlow(new ReceiveFinalityFlow(otherSide));

        return null;
    }
}