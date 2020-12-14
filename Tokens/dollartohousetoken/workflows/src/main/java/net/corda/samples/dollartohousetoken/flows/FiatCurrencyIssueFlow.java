package net.corda.samples.dollartohousetoken.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.intellij.lang.annotations.Flow;

/**
 * Flow class to issue fiat currency. FiatCurrency is defined in the Token SDK and is issued as a Fungible Token.
 * This constructor takes the currency code for the currency to be issued, the amount of the currency to be issued
 * and the recipient as input parameters.
 */
@StartableByRPC
public class FiatCurrencyIssueFlow extends FlowLogic<SignedTransaction> {

    private final String currency;
    private final Long amount;
    private final Party recipient;

    public FiatCurrencyIssueFlow(String currency, Long amount, Party recipient) {
        this.currency = currency;
        this.amount = amount;
        this.recipient = recipient;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        /* Create an instance of the fiat currency token */
        TokenType tokenType = getTokenType();

        /* Create an instance of FungibleToken for the fiat currency to be issued */
        FungibleToken fungibleToken =
                new FungibleTokenBuilder()
                    .ofTokenType(tokenType)
                    .withAmount(amount)
                    .issuedBy(getOurIdentity())
                    .heldBy(recipient)
                    .buildFungibleToken();

        /* Issue the required amount of the token to the recipient */
        return subFlow(new IssueTokens(ImmutableList.of(fungibleToken), ImmutableList.of(recipient)));
    }

    private TokenType getTokenType() throws FlowException{
        switch (currency){
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
