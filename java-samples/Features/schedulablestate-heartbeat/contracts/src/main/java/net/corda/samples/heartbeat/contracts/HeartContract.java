package net.corda.samples.heartbeat.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

/**
 * A blank contract and command, solely used for building a valid Heartbeat state transaction.
 */
public class HeartContract implements Contract {
    public final static String contractID = "net.corda.samples.heartbeat.contracts.HeartContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Omitted for the purpose of this sample.
    }

    public interface Commands extends CommandData {
        class Beat implements Commands {}
    }
}
