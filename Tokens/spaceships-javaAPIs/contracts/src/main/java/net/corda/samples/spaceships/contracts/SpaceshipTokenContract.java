package net.corda.samples.spaceships.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.spaceships.states.SpaceshipTokenType;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class SpaceshipTokenContract extends EvolvableTokenContract implements Contract {

    public static final String CONTRACT_ID = "net.corda.samples.spaceships.contracts.SpaceshipTokenContract";

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        SpaceshipTokenType newToken = (SpaceshipTokenType) tx.getOutputStates().get(0);
        requireThat( require -> {
            require.using("Planet Of Origin cannot be empty",(!newToken.getPlanetOfOrigin().equals("")));
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        /*This additional check does not apply to this use case.
         *This sample does not allow token update */
    }
}
