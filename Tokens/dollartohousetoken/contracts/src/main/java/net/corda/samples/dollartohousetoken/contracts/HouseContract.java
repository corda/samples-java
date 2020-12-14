package net.corda.samples.dollartohousetoken.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.samples.dollartohousetoken.states.HouseState;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/*
*  HouseContract governs the evolution of HouseState token. Evolvable tokens must extend the EvolvableTokenContract abstract class, it defines the
*  additionalCreateChecks and additionalCreateChecks method to add custom logic to validate while creation and update of evolvable tokens respectively.
* */
public class HouseContract extends EvolvableTokenContract implements Contract {

    public static final String CONTRACT_ID = "net.corda.samples.dollartohousetoken.contracts.HouseContract";

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while creation of token
        HouseState outputState = (HouseState) tx.getOutput(0);
        requireThat( require -> {
            require.using("Valuation cannot be zero",
                    outputState.getValuation().getQuantity() > 0);
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while updation of token
    }

}
