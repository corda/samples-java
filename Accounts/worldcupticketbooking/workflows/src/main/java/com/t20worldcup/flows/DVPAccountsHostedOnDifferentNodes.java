package com.t20worldcup.flows;


import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import com.t20worldcup.states.T20CricketTicket;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlow;
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.TokenQueryBy;
import com.r3.corda.lib.tokens.selection.database.config.DatabaseSelectionConfigKt;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import kotlin.Pair;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.*;


/**
 * This flow shows you how to perform a dvp between accounts hosted on different nodes. Buyer account is on one host and Seller account is on other node.
 * Majority of the code which deals with building the transaction for dvp is similar to DVPAccountsOnSameNode.java.
 * Few more things to keep in mind - when accounts hosted on different nodes want to transact with each other you need to make sure to
 * share the keys which account on one node must created and used. This is done as the other node/account is not aware which key was used to sign
 * a shared transaction. To verify this, Node1 needs to share the keys. This can be done by calling SyncKeyMappingFlow. This flow will have to be called from
 * both the sides. Once we sync accounts and keys and built the transaction, we can then call CollectSignaturesFlow.
 * Look at how I am passing the 4 th parameter to CollectSignaturesFlow which tells the counterparty which all keys were used by the initiator for signing,
 * so the counterparty will use remaining keys to sign the transaction. There is one more tricky bit tto this. When we call MoveTokensUtilitiesKt.addMoveTokens,
 * internally a new Anonymous key is created and assigned to the current holder. So internally as well token sdk uses Confidential Identity. We will have to
 * explicitly sync this key as well, as the counterparty is not aware of this key.
 */
@StartableByRPC
@InitiatingFlow
public class DVPAccountsHostedOnDifferentNodes extends FlowLogic<String> {

    private final String tokenId;
    private final String buyerAccountName;
    private final String sellerAccountName;
    private final String currency;
    private final Long costOfTicket;

    public DVPAccountsHostedOnDifferentNodes(String tokenId, String buyerAccountName, String sellerAccountName, Long costOfTicket, String currency) {
        this.tokenId = tokenId;
        this.buyerAccountName = buyerAccountName;
        this.sellerAccountName = sellerAccountName;
        this.costOfTicket = costOfTicket;
        this.currency = currency;
    }


    //This flow will be initiated by the buyer who wishes to buy the ticket from an account hosted on different node.
    //The buyer will call the generate move token , generate the cash token transfer states and send it to selelr.
    @Override
    @Suspendable
    public String call() throws FlowException {

        //Get buyers and sellers account infos
        AccountInfo buyerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(buyerAccountName).get(0).getState().getData();
        AccountInfo sellerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(sellerAccountName).get(0).getState().getData();

        //Generate new keys for buyers and sellers
        //make sure to sync these keys with the counterparty by calling SyncKeyMappingFlow as below
        AnonymousParty buyerAccount = subFlow(new RequestKeyForAccount(buyerAccountInfo));//mappng saved locally
        AnonymousParty sellerAccount = subFlow(new RequestKeyForAccount(sellerAccountInfo));//mappiing requested from counterparty. does the counterparty save i dont think so


        //Part1 : Move non fungible token - ticket from seller to buyer
        //establish session with seller
        FlowSession sellerSession = initiateFlow(sellerAccountInfo.getHost());

        //send uuid, buyer,seller account name to seller
        sellerSession.send(tokenId);
        sellerSession.send(buyerAccountName);
        sellerSession.send(sellerAccountName);

        //buyer will create generate a move tokens state and send this state with new holder(seller) to seller
        Amount<FiatCurrency> amount = new Amount(costOfTicket, FiatCurrency.Companion.getInstance(currency));

        //Buyer Query for token balance.
        QueryCriteria queryCriteria = QueryUtilitiesKt.heldTokenAmountCriteria(this.getInstance(currency), buyerAccount).and(QueryUtilitiesKt.sumTokenCriteria());
        List<Object> sum = getServiceHub().getVaultService().queryBy(FungibleToken.class, queryCriteria).component5();
        if(sum.size() == 0)
            throw new FlowException(buyerAccountName + " has 0 token balance. Please ask the Bank to issue some cash.");
        else {
            Long tokenBalance = (Long) sum.get(0);
            if(tokenBalance < costOfTicket)
                throw new FlowException("Available token balance of " + buyerAccountName+ " is less than the cost of the ticket. Please ask the Bank to issue some cash if you wish to buy the ticket ");
        }

        //the tokens to move to new account which is the seller account
        Pair<AbstractParty, Amount<TokenType>> partyAndAmount = new Pair(sellerAccount, amount);

        //let's use the DatabaseTokenSelection to get the tokens from the db
        DatabaseTokenSelection tokenSelection = new DatabaseTokenSelection(
                getServiceHub(),
                DatabaseSelectionConfigKt.MAX_RETRIES_DEFAULT,
                DatabaseSelectionConfigKt.RETRY_SLEEP_DEFAULT,
                DatabaseSelectionConfigKt.RETRY_CAP_DEFAULT,
                DatabaseSelectionConfigKt.PAGE_SIZE_DEFAULT
        );

        //call generateMove which gives us 2 stateandrefs with tokens having new owner as seller.
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs =
                tokenSelection.generateMove(Arrays.asList(partyAndAmount), buyerAccount, new TokenQueryBy(), getRunId().getUuid());

        //send the generated inputsAndOutputs to the seller
        subFlow(new SendStateAndRefFlow(sellerSession, inputsAndOutputs.getFirst()));
        sellerSession.send(inputsAndOutputs.getSecond());

        //sync following keys with seller - buyeraccounts, selleraccounts which we generated above using RequestKeyForAccount, and IMP: also share the anonymouse keys
        //created by the above token move method for the holder.
        List<AbstractParty> signers = new ArrayList<>();
        signers.add(buyerAccount);
        signers.add(sellerAccount);

        List<StateAndRef<FungibleToken>> inputs = inputsAndOutputs.getFirst();
        for(StateAndRef<FungibleToken> tokenStateAndRef : inputs) {
            signers.add(tokenStateAndRef.getState().getData().getHolder());
        }

        //Sync our associated keys with the conterparties.
        subFlow(new SyncKeyMappingFlow(sellerSession, signers));

        //this is the handler for synckeymapping called by seller. seller must also have created some keys not known to us - buyer
        subFlow(new SyncKeyMappingFlowHandler(sellerSession));

        //recieve the data from counter session in tx formatt.
        subFlow(new SignTransactionFlow(sellerSession) {
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        SignedTransaction stx = subFlow(new ReceiveFinalityFlow(sellerSession));
        return ("The ticket is sold to "+buyerAccountName+""+ "\ntxID: " + stx.getId().toString());
    }

    public TokenType getInstance(String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        return new TokenType(currency.getCurrencyCode(), 0);
    }
}



//seller will get the token cash transfer state from buyer. seller will generate the non fungible transfer - ticket transfer state. Seller will add these two
//inouts and outputs to a transaction and perform dvp.
@InitiatedBy(DVPAccountsHostedOnDifferentNodes.class)
class DVPAccountsHostedOnDifferentNodesResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public DVPAccountsHostedOnDifferentNodesResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        //get all the details from the seller
        String tokenId = otherSide.receive(String.class).unwrap(t->t);
        String buyerAccountName = otherSide.receive(String.class).unwrap(t->t);
        String sellerAccountName = otherSide.receive(String.class).unwrap(t->t);

        List<StateAndRef<FungibleToken>> inputs =  subFlow(new ReceiveStateAndRefFlow<>(otherSide));
        List<FungibleToken> moneyReceived = otherSide.receive(List.class).unwrap(value -> value);

        //call SyncKeyMappingHandler for SyncKey Mapping called at buyers side
        subFlow(new SyncKeyMappingFlowHandler(otherSide));

        //Get buyers and sellers account infos
        AccountInfo buyerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(buyerAccountName).get(0).getState().getData();
        AccountInfo sellerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(sellerAccountName).get(0).getState().getData();

        //Generate new keys for buyers and sellers
        AnonymousParty buyerAccount = subFlow(new RequestKeyForAccount(buyerAccountInfo));
        AnonymousParty sellerAccount = subFlow(new RequestKeyForAccount(sellerAccountInfo));

        //query for all tickets
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

        //Query for the ticket Buyer wants to sell.
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Arrays.asList(UUID.fromString(ticketId)), null,
                Vault.StateStatus.UNCONSUMED, null);

        //grab the t20worldcup off the ledger
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

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        //part1 of DVP is to transfer the non fungible token from seller to buyer
        MoveTokensUtilitiesKt.addMoveNonFungibleTokens(transactionBuilder, getServiceHub(), tokenPointer, buyerAccount);

        //part2 of DVP is to transfer cash - fungible token from buyer to seller and return the change to buyer
        MoveTokensUtilitiesKt.addMoveTokens(transactionBuilder, inputs, moneyReceived);

        //sync keys with buyer, again sync for similar members

        List<AbstractParty> signers = new ArrayList<>();
        signers.add(buyerAccount);
        signers.add(sellerAccount);

        for(StateAndRef<FungibleToken> tokenStateAndRef : inputs) {
            signers.add(tokenStateAndRef.getState().getData().getHolder());
        }

        subFlow(new SyncKeyMappingFlow(otherSide, signers));

        //call filterMyKeys to get the my signers for seller node and pass in as a 4th parameter to CollectSignaturesFlow.
        //by doing this we tell CollectSignaturesFlow that these are the signers which have already signed the transaction
        List<CommandWithParties<CommandData>> commandWithPartiesList  = transactionBuilder.toLedgerTransaction(getServiceHub()).getCommands();

        List<PublicKey> mySigners = new ArrayList();

        for(CommandWithParties<CommandData> commandDataCommandWithParties : commandWithPartiesList) {
            if(((ArrayList<PublicKey>)(getServiceHub().getKeyManagementService().filterMyKeys(commandDataCommandWithParties.getSigners()))).size() > 0) {
                mySigners.add(((ArrayList<PublicKey>)getServiceHub().getKeyManagementService().filterMyKeys(commandDataCommandWithParties.getSigners())).get(0));
            }
        }

        //sign the transaction with the signers we got by calling filterMyKeys
        SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder, mySigners);

        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                selfSignedTransaction,
                Collections.singletonList(otherSide),
                mySigners));

        //call FinalityFlow for finality
        subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherSide)));

        return null;
    }
}


