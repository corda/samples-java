package net.corda.examples.spaceships.flows;

import com.google.common.collect.ImmutableMap;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.examples.spaceships.states.SpaceshipTokenType;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public interface FlowHelpers {
    // Base rate is USD 1:1
    double USDRate = 1.00;
    double AUDRate = 0.60;
    double GBPRate = 1.30;
    double BTCRate = 1000.00;
    Map<String, Double> rates = ImmutableMap.of("USD", USDRate, "AUD", AUDRate, "GBP", GBPRate, "Bitcoin", BTCRate);

    /**
     * exchangeCurrency calculates an amount back in targetCurrency that is fair exchange with an itemCurrency
     * This allows a buyer to know how much of his held currency to give to a seller to satisfy the price.
     * @param amount
     * @param targetCurrency
     * @return
     */
    static Amount<TokenType> exchangeCurrency(Amount<TokenType> amount, TokenType targetCurrency) {
        int itemCurrFractionDigits = amount.getToken().getFractionDigits();
        double newValue = (amount.getQuantity()/Math.pow(10,itemCurrFractionDigits)) * (rates.get(amount.getToken().getTokenIdentifier()) / rates.get(targetCurrency.getTokenIdentifier()));
        return AmountUtilities.amount(newValue, targetCurrency);
    }

    /**
     * parseAmountFromString takes in a String representation of an Amount (useful for terminal / UI input)
     * and returns the correct object.
     * @param value
     * @return
     */
    static Amount<TokenType> parseAmountFromString(String value) {
        String[] in = value.split(" ");
        double val = Long.parseLong(in[0]);
        String curr = in[1];
        if (curr.equals("BTC")) return AmountUtilities.amount(val, DigitalCurrency.getInstance("BTC"));
        else return AmountUtilities.amount(val, FiatCurrency.getInstance(curr));
    }

    static SpaceshipTokenType uuidToSpaceShipTokenType(VaultService vs, UUID uuid) {
        // Get the state definition from vault to grab the value
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Collections.singletonList(uuid));
        StateAndRef<SpaceshipTokenType> spaceShip = vs.queryBy(SpaceshipTokenType.class, queryCriteria).getStates().get(0);

        return spaceShip.getState().getData();
    }

}
