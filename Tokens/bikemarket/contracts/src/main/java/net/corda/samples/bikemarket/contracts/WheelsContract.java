package net.corda.samples.bikemarket.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.bikemarket.states.WheelsTokenState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class WheelsContract extends EvolvableTokenContract implements Contract {

    public static final String CONTRACT_ID = "net.corda.samples.bikemarket.contracts.WheelsContract";

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        WheelsTokenState newToken = (WheelsTokenState) tx.getOutputStates().get(0);
        requireThat( require -> {
            require.using("Serial Number cannot be empty",(!newToken.getserialNum().equals("")));
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        /*This additional check does not apply to this use case.
         *This sample does not allow token update */
    }
}
