package net.corda.samples.bikemarket.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.bikemarket.states.FrameTokenState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class FrameContract extends EvolvableTokenContract implements Contract {

    public static final String CONTRACT_ID = "net.corda.samples.bikemarket.contracts.FrameContract";

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        FrameTokenState newToken = (FrameTokenState) tx.getOutputStates().get(0);
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
