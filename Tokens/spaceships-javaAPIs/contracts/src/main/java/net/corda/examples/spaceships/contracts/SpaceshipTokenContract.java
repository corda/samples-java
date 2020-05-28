package net.corda.examples.spaceships.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class SpaceshipTokenContract extends EvolvableTokenContract implements Contract {

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        // add additional create checks here
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        // add additional update checks here
    }
}
