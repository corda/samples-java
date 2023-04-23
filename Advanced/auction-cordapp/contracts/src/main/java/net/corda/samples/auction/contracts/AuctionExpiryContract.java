package net.corda.samples.auction.contracts;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.auction.states.AuctionExpiry;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class AuctionExpiryContract implements Contract {
    public static final String ID = "net.corda.samples.auction.contracts.AuctionExpiryContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        if(tx.getCommands().size() == 0){
            throw new IllegalArgumentException("One command Expected");
        }

        Command<CommandData> command = tx.getCommand(0);

        if(command.getValue() instanceof AuctionContract.Commands.CreateAuction) {
            if(tx.outputsOfType(AuctionExpiry.class).size() != 1){
                throw new IllegalArgumentException("Exactly one expiry output state expected");
            }
            AuctionExpiry expiry = tx.outputsOfType(AuctionExpiry.class).get(0);
            if(expiry.getExpiry() == null){
                throw new IllegalArgumentException("Make sure you specify the expiry for the Auction.");
            }

        }else if(command.getValue() instanceof AuctionContract.Commands.Bid){
            if(tx.outputsOfType(AuctionExpiry.class).size() != 1 &&
                    tx.inputsOfType(AuctionExpiry.class).size() != 1){
                throw new IllegalArgumentException("Exactly one expiry output and input state expected");
            }
            AuctionExpiry expiryOutput = tx.outputsOfType(AuctionExpiry.class).get(0);
            AuctionExpiry expiryInput = tx.inputsOfType(AuctionExpiry.class).get(0);

            if(!expiryInput.getExpiry().equals(expiryOutput.getExpiry())){
                throw new IllegalArgumentException("Auction expiry should not change while placing a bid");
            }

            if(!expiryInput.getAuctionId().equals(expiryOutput.getAuctionId()) &&
                    !expiryInput.getParticipants().equals(expiryOutput.getParticipants())){
                throw new IllegalArgumentException("Auction Id and Par should not change while placing a bid");
            }
        }else if(command.getValue() instanceof AuctionContract.Commands.EndAuction){
            if(tx.inputsOfType(AuctionExpiry.class).size() != 1){
                throw new IllegalArgumentException("Exactly one expiry input state expected");
            }
            AuctionExpiry expiry = tx.inputsOfType(AuctionExpiry.class).get(0);

            TimeWindow timeWindow = tx.getTimeWindow();
            if (timeWindow == null || timeWindow.getFromTime() == null) {
                throw new IllegalArgumentException("Make sure you specify the time window for this transaction.");
            }
            if (Objects.requireNonNull(timeWindow.getFromTime()).isBefore(expiry.getExpiry())) {
                throw new IllegalArgumentException("Auction is still active, it expires at: " +
                        LocalDateTime.ofInstant(expiry.getExpiry(), ZoneId.systemDefault()));
            }
        }
        else{
            throw new IllegalArgumentException("Invalid Command");
        }
    }

    public interface Commands extends CommandData {
        class Create implements Commands { }
    }
}
