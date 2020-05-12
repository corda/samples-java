package net.corda.examples.spaceships.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Arrays;

public interface IssuePlanetaryCurrencyFlows {

    @StartableByRPC
    class Initiator extends FlowLogic<SignedTransaction> {

        private final Party holder;
        private final String currencyCode;
        private final int amount;

        public Initiator(Party holder, String currencyCode, int amount) {
            this.holder = holder;
            this.currencyCode = currencyCode;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            final Party issuer = getOurIdentity();
            TokenType tokenType;

            switch (currencyCode) {
                case "USD":
                    // MoneyUtilities can return either a TokenType or Amount<TokenType> related to standard currencies
                    tokenType = MoneyUtilities.getUSD();
                    break;
                case "AUD":
                    tokenType = MoneyUtilities.getAUD();
                    break;
                case "GBP":
                    // FiatCurrency returns a TokenType from an ISO currency code
                    tokenType = FiatCurrency.getInstance(currencyCode);
                    break;
                case "BTC":
                    // DigitalCurrency returns a TokenType related to standard crypto/digital currencies
                    tokenType = DigitalCurrency.getInstance(currencyCode);
                    break;
                default:
                    throw new FlowException("unable to generate currency");
            }

            // The FungibleTokenBuilder allows quick and easy stepwise assembly of your token.
            FungibleToken tokens = new FungibleTokenBuilder()
                    .ofTokenType(tokenType)
                    .withAmount(amount)
                    .issuedBy(issuer)
                    .heldBy(holder)
                    .buildFungibleToken();

            return subFlow(new IssueTokens(Arrays.asList(tokens)));
        }
    }
}
