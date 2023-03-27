package net.corda.samples.avatar.contracts;

import net.corda.samples.avatar.states.Expiry;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;

//ExpiryContract is also run when Avatar's contract is run
public class ExpiryContract implements Contract {

    public static final String EXPIRY_CONTRACT_ID = "net.corda.samples.avatar.contracts.ExpiryContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        Expiry expiry;
        if (tx.getCommands().stream().anyMatch(e -> e.getValue() instanceof AvatarContract.Commands.Transfer))
            expiry = tx.inputsOfType(Expiry.class).get(0);
        else
            expiry = tx.outputsOfType(Expiry.class).get(0);

        TimeWindow timeWindow = tx.getTimeWindow();
        if (timeWindow == null || timeWindow.getUntilTime() == null) {
            throw new IllegalArgumentException("Make sure you specify the time window for the Avatar transaction.");
        }

        //Expiry time should be after the time window, if the avatar expires before the time window, then the avatar
        //cannot be sold
        if (timeWindow.getUntilTime().isAfter(expiry.getExpiry())) {
            throw new IllegalArgumentException("Avatar transfer time has expired! Expiry date & time was: " + LocalDateTime.ofInstant(expiry.getExpiry(), ZoneId.systemDefault()));
        }
    }

    public interface Commands extends CommandData {
        class Create implements Commands {
        }

        class Pass implements Commands {
        }
    }
}
