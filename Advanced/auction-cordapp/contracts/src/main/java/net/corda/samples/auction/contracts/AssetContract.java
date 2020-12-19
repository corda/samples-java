package net.corda.samples.auction.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

//TODO
public class AssetContract implements Contract {
    // This is used to identify our contracts when building a transaction.
    public static final String ID = "net.corda.samples.auction.contracts.AssetContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        // Contract Verification code goes here. Left blank for simplicity
    }

    public interface Commands extends CommandData {
        class CreateAsset implements Commands {}
        class TransferAsset implements Commands {}
    }
}
