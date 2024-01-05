package net.corda.samples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.vault.QueryCriteria;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import kotlin.Pair;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/*
 * This flow transfers the equivalent amount of fiat currency to the seller.
 */
@StartableByRPC
public class FiatCurrencyMoveFlow extends FlowLogic<SignedTransaction> {

    private final String currency;
    private final Long amount;
    private final Party recipient;

    public FiatCurrencyMoveFlow(String currency, Long amount, Party recipient) {
        this.currency = currency;
        this.amount = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        Optional<Long> optFoo = Optional.ofNullable(null);
        long longAmount = optFoo.orElse( this.amount );
        /* Create instance of the fiat currency token amount */
        Amount<TokenType> tokenAmount = new Amount<>(longAmount, getTokenType());

        System.out.println("recipient: " + this.recipient);
        System.out.println("amount: " + this.amount);
        System.out.println("longAmount: " + longAmount);
        System.out.println("currency: " + this.currency);
        System.out.println("tokenAmount: " + tokenAmount);

        /* Generate the move proposal, it returns the input-output pair for the fiat currency transfer, which we need to
        send to the Initiator */
        Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> inputsAndOutputs = new DatabaseTokenSelection(getServiceHub())
                // here we are generating input and output states which send the correct amount to the seller, and any change back to buyer
                .generateMove(Collections.singletonList(new Pair<>(this.recipient, tokenAmount)), getOurIdentity());

        System.out.println("inputsAndOutputs: " + inputsAndOutputs);
        
        final TokenType usdTokenType = FiatCurrency.Companion.getInstance("USD");
        final Party partyA = getServiceHub().getNetworkMapCache().getPeerByLegalName(
                CordaX500Name.parse("O=PartyA,L=London,C=GB"));
        final Party holderMint = getServiceHub().getNetworkMapCache().getPeerByLegalName(
                CordaX500Name.parse("O=PartyA,L=London,C=GB"));
        if (holderMint == null)
            throw new FlowException("No Mint found");

        // Who is going to own the output, and how much?
        final Party partyC = getServiceHub().getNetworkMapCache().getPeerByLegalName(
                CordaX500Name.parse("O=PartyC, L=Mumbai, C=IN"));
        final Amount<TokenType> fiftyUSD = AmountUtilities.amount(50L, usdTokenType);
        final PartyAndAmount<TokenType> fiftyUSDForPartyC = new PartyAndAmount<>(partyC, tokenAmount);

        // Describe how to find those $ held by PartyA.
        final QueryCriteria issuedByHolderMint = QueryUtilities.tokenAmountWithIssuerCriteria(usdTokenType, holderMint);
        final QueryCriteria heldByPartyA = QueryUtilities.heldTokenAmountCriteria(usdTokenType, partyA);

        // Do the move
        final SignedTransaction moveTx = subFlow(new MoveFungibleTokens(
                Collections.singletonList(fiftyUSDForPartyC), // Output instances
                Collections.emptyList(), // Observers
                issuedByHolderMint.and(heldByPartyA), // Criteria to find the inputs
                partyA)); // change holder
        return moveTx;
    }

    private TokenType getTokenType() throws FlowException {
        switch (currency) {
            case "USD":
                return MoneyUtilities.getUSD();

            case "GBP":
                return MoneyUtilities.getGBP();

            case "EUR":
                return MoneyUtilities.getEUR();

            default:
                throw new FlowException("Currency Not Supported");
        }
    }
}
