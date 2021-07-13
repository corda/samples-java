package com.t20worldcup.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.UtilitiesKt;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.selection.TokenQueryBy;
import com.r3.corda.lib.tokens.selection.database.config.DatabaseSelectionConfigKt;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.security.PublicKey;
import java.util.*;

/**
 * This flow only talks about moving fungible tokens from one account to other.
 */
@StartableByRPC
@InitiatingFlow
public class MoveTokensBetweenAccounts extends FlowLogic<String> {

    private final String buyerAccountName;
    private final String sellerAccountName;
    private final String currency;
    private final Long costOfTicket;

    public MoveTokensBetweenAccounts(String buyerAccountName, String sellerAccountName, String currency, Long costOfTicket) {
        this.buyerAccountName = buyerAccountName;
        this.sellerAccountName = sellerAccountName;
        this.currency = currency;
        this.costOfTicket = costOfTicket;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        //Get buyers and sellers account infos
        AccountInfo buyerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(buyerAccountName).get(0).getState().getData();
        AccountInfo sellerAccountInfo = UtilitiesKt.getAccountService(this).accountInfo(sellerAccountName).get(0).getState().getData();

        //Generate new keys for buyers and sellers
        AnonymousParty buyerAccount = subFlow(new RequestKeyForAccount(buyerAccountInfo));//mappng saved locally
        AnonymousParty sellerAccount = subFlow(new RequestKeyForAccount(sellerAccountInfo));//mappiing requested from counterparty. does the counterparty save i dont think so

        //buyer will create generate a move tokens state and send this state with new holder(seller) to seller
        Amount<TokenType> amount = new Amount(costOfTicket, getInstance(currency));

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

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder transactionBuilder = new TransactionBuilder(notary);

        MoveTokensUtilitiesKt.addMoveTokens(transactionBuilder, inputsAndOutputs.getFirst(), inputsAndOutputs.getSecond());

        Set<PublicKey> mySigners = new HashSet<>();

        List<CommandWithParties<CommandData>> commandWithPartiesList  = transactionBuilder.toLedgerTransaction(getServiceHub()).getCommands();

        for(CommandWithParties<CommandData> commandDataCommandWithParties : commandWithPartiesList) {
            if(((ArrayList<PublicKey>)(getServiceHub().getKeyManagementService().filterMyKeys(commandDataCommandWithParties.getSigners()))).size() > 0) {
                mySigners.add(((ArrayList<PublicKey>)getServiceHub().getKeyManagementService().filterMyKeys(commandDataCommandWithParties.getSigners())).get(0));
            }
        }

        FlowSession sellerSession = initiateFlow(sellerAccountInfo.getHost());

        //sign the transaction with the signers we got by calling filterMyKeys
        SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder, mySigners);

        //call FinalityFlow for finality
        subFlow(new FinalityFlow(selfSignedTransaction, Arrays.asList(sellerSession)));

        return null;
    }

    public TokenType getInstance(String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        return new TokenType(currency.getCurrencyCode(), 0);
    }
}

@InitiatedBy(MoveTokensBetweenAccounts.class)
class MoveTokensBetweenAccountsResponder extends FlowLogic<Void> {

    private final FlowSession otherSide;

    public MoveTokensBetweenAccountsResponder(FlowSession otherSide) {
        this.otherSide = otherSide;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {

        subFlow(new ReceiveFinalityFlow(otherSide));

        return null;
    }
}