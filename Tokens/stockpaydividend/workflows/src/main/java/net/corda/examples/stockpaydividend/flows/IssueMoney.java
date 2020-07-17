package net.corda.examples.stockpaydividend.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

/**
 * Designed initiating node : Bank
 */
@InitiatingFlow
@StartableByRPC
public class IssueMoney extends FlowLogic<String> {

    private String currency;
    private Long quantity;
    private Party recipient;

    public IssueMoney(String currency, Long amount, Party recipient) {
        this.currency = currency;
        this.quantity = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {

        // Create an instance of the fiat currency token type
        TokenType token = FiatCurrency.Companion.getInstance(currency);



        // Create an instance of FungibleToken for the fiat currency to be issued
        //new FungibleToken(new Amount<>(quantity, issuedTokenType), recipient, null);
        FungibleToken fungibleToken = new FungibleTokenBuilder()
                .ofTokenType(token)
                .withAmount(this.quantity)
                .issuedBy(getOurIdentity())
                .heldBy(recipient)
                .buildFungibleToken();

        // Use the build-in flow, IssueTokens, to issue the required amount to the the recipient
        SignedTransaction stx = subFlow(new IssueTokens(ImmutableList.of(fungibleToken), ImmutableList.of(recipient)));
        return "\nIssued to " + recipient.getName().getOrganisation() + " " + this.quantity +" "
                + this.currency +" for stock issuance."+ "\nTransaction ID: "+ stx.getId();
    }
}
