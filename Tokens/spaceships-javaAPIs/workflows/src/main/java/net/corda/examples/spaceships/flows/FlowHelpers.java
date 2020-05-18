package net.corda.examples.spaceships.flows;

import com.google.common.collect.ImmutableMap;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.contracts.Amount;

import java.util.Map;

public interface FlowHelpers {
    // Base rate is USD
    double USDRate = 1.00;
    double AUDRate = 0.60;
    double GBPRate = 1.30;
    double BTCRate = 1000.00;
    Map<String, Double> rates = ImmutableMap.of("USD", USDRate, "AUD", AUDRate, "GBP", GBPRate, "Bitcoin", BTCRate);

    /**
     * exchangeCurrency calculates an amount back in heldCurrency that is fair exchange with an itemCurrency
     * This allows a buyer to know how much of his held currency to give to a seller to satisfy the price.
     * @param itemAmount
     * @param heldCurrency
     * @return
     */
    static Amount<TokenType> exchangeCurrency(Amount<TokenType> itemAmount, TokenType heldCurrency) {
        int itemCurrFractionDigits = itemAmount.getToken().getFractionDigits();
        double newValue = (itemAmount.getQuantity()/Math.pow(10,itemCurrFractionDigits)) * (rates.get(itemAmount.getToken().getTokenIdentifier()) / rates.get(heldCurrency.getTokenIdentifier()));
        return AmountUtilities.amount(newValue, heldCurrency);
    }

    static Amount<TokenType> parseValueFromString(String value) {
        String[] in = value.split(" ");
        double val = Long.parseLong(in[0]);
        String curr = in[1];
        if (curr.equals("BTC")) return AmountUtilities.amount(val, DigitalCurrency.getInstance("BTC"));
        else return AmountUtilities.amount(val, FiatCurrency.getInstance(curr));
    }

//    public static void main(String[] args) {
//        Amount<TokenType> testVar = parseValueFromString("1 USD");
//        Amount<TokenType> convertedVar = exchangeCurrency(testVar, FiatCurrency.getInstance("GBP"));
////        Amount<TokenType> convertedVar = exchangeCurrency(testVar, DigitalCurrency.getInstance("BTC"));
//        System.out.println(convertedVar.getQuantity());
//    }

}
