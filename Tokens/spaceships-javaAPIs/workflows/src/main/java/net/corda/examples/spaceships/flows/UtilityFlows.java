package net.corda.examples.spaceships.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.selection.TokenQueryBy;
import com.r3.corda.lib.tokens.selection.api.Selector;
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.VaultService;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classes for running various types of Querying and Return functions
 */
public interface UtilityFlows {

    /**
     * LooseChangeFinder
     * returns all tokens of a certain TokenType which has fractional values
     * This demonstrates how lambda predicates can be passed to TokenQueryBY in order to have
     * very precise filtering with your chosen token selector
     */
    @StartableByRPC
    class LooseChangeFinderFlow extends FlowLogic<List<StateAndRef<FungibleToken>>> {
        private final TokenType tokenType;

        /**
         * LooseChangeFinder
         * @param tokenType - the TokenType you would like to find loose change for
         */
        public LooseChangeFinderFlow(TokenType tokenType) {
            this.tokenType = tokenType;
        }

        @Override
        public List<StateAndRef<FungibleToken>> call() throws FlowException {
            Amount<TokenType> totalAmountHeld = QueryUtilities.tokenBalance(getServiceHub().getVaultService(), tokenType);
            TokenQueryBy tokenQueryBy = new TokenQueryBy(
                    null,
                    it -> it.getState().getData().getAmount().toDecimal().stripTrailingZeros().scale() > 0
            );
            Selector selector = new DatabaseTokenSelection(getServiceHub());
            return selector.selectTokens(totalAmountHeld, tokenQueryBy);
        }
    }

    /**
     * GetTotalNetWorthFlow
     * Returns a single Amount in a given TokenType/Currency which is the adjusted amount of
     * all Fungible assets of the holder (after currency conversions)
     * This flow demonstrates use of AccountUtilities and QueryUtilities working in tandem
     * to accomplish a more complex operation on a collection of tokens
     */
    @StartableByRPC
    class GetTotalNetWorthFlow extends FlowLogic<Amount<TokenType>>  {

        private final TokenType outputTokenType;
        private final List<TokenType> exclusionList;

        /**
         * GetTotalNetWorthFlow
         * @param outputTokenType - networth returned in Amount<outputTokenType>
         * @param exclusionList - TokenTypes to ignore when calculating worth.
         */
        public GetTotalNetWorthFlow(TokenType outputTokenType, List<TokenType> exclusionList) {
            this.outputTokenType = outputTokenType;
            this.exclusionList = exclusionList;
        }

        public GetTotalNetWorthFlow(TokenType outputTokenType) {
            this(outputTokenType, null);
        }

        @Suspendable
        @Override
        public Amount<TokenType> call() throws FlowException {
            VaultService vs = getServiceHub().getVaultService();

            // Make sure outputTokenType is valid
            if (!FlowHelpers.rates.containsKey(outputTokenType.getTokenIdentifier())) throw new FlowException("Invalid target TokenType");

            Set<TokenType> tokenTypesAvailable = fetchTokenTypes(FlowHelpers.rates.keySet());

            // Remove excluded Tokens (will not be in calculation)
            if (exclusionList != null) tokenTypesAvailable.removeAll(exclusionList);

            List<Amount<TokenType>> amountsOfOutputTokenType = new ArrayList<>();
            tokenTypesAvailable.forEach(it -> {
                Amount<TokenType> amountOfCurrentToken = QueryUtilities.tokenBalance(vs, it);
                // Convert to output currency
                if (!it.equals(outputTokenType)) {
                    amountOfCurrentToken = FlowHelpers.exchangeCurrency(amountOfCurrentToken, outputTokenType);
                }
                amountsOfOutputTokenType.add(amountOfCurrentToken);
            });

            return AmountUtilities.sumTokensOrThrow(amountsOfOutputTokenType);
        }

        private static Set<TokenType> fetchTokenTypes(Set<String> identifiers) {
            return identifiers.stream().map(it -> {
                try {
                    return FiatCurrency.getInstance(it);
                } catch (IllegalArgumentException e) {
                    return DigitalCurrency.getInstance("BTC");
                }
            }).collect(Collectors.toSet());
        }
    }
}
