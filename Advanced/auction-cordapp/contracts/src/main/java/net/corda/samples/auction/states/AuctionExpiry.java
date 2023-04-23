package net.corda.samples.auction.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.SchedulableState;
import net.corda.core.contracts.ScheduledActivity;
import net.corda.core.contracts.StateRef;
import net.corda.core.flows.FlowLogicRef;
import net.corda.core.flows.FlowLogicRefFactory;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.auction.contracts.AuctionExpiryContract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@BelongsToContract(AuctionExpiryContract.class)
public class AuctionExpiry implements ContractState, SchedulableState {

    private final Instant expiry;
    private final UUID auctionId;
    private final List<Party> participants;

    public AuctionExpiry(Instant expiry, UUID auctionId, List<Party> participants) {
        this.expiry = expiry;
        this.auctionId = auctionId;
        this.participants = participants;
    }

    /**
      * This method returns a ScheduledActivity. The ScheduledActivity encapsulates a flows ref and a trigger time
      * to start the ScheduledFlow.
      *
      * @param thisStateRef
      * @param flowLogicRefFactory
      * @return a ScheduledActivity to be triggered at a specific instant.
     */
    @Nullable
    @Override
    public ScheduledActivity nextScheduledActivity(@NotNull StateRef thisStateRef,
                                                   @NotNull FlowLogicRefFactory flowLogicRefFactory) {

        FlowLogicRef flowLogicRef = flowLogicRefFactory.create(
                "net.corda.samples.auction.flows.EndAuctionFlow$EndAuctionInitiator", auctionId);
        return new ScheduledActivity(flowLogicRef, expiry);
    }

    public Instant getExpiry() {
        return expiry;
    }

    public UUID getAuctionId() {
        return auctionId;
    }



    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> parties = new ArrayList<>(participants);
        return parties;
    }

}
