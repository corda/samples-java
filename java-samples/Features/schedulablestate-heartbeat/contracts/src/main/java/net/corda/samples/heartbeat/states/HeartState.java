package net.corda.samples.heartbeat.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.SchedulableState;
import net.corda.core.contracts.ScheduledActivity;
import net.corda.core.contracts.StateRef;
import net.corda.core.flows.FlowLogicRefFactory;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.heartbeat.contracts.HeartContract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Every Heartbeat state has a scheduled activity to start a flow to consume itself and produce a
 * new Heartbeat state on the ledger after five seconds.
 *
 * @property me The creator of the Heartbeat state.
 * @property nextActivityTime When the scheduled activity should be kicked off.
 */
@BelongsToContract(HeartContract.class)
public class HeartState implements SchedulableState {

    private final Party me;
    private final Instant nextActivityTime;

    public HeartState(Party me) {
        this.me = me;
        this.nextActivityTime = Instant.now().plusSeconds(1);
    }

    // Scheduleable state must have a constructor with nextActivity time for
    // deserialization
    @ConstructorForDeserialization
    public HeartState(Party me, Instant nextActivityTime) {
        this.me = me;
        this.nextActivityTime = nextActivityTime;
    }

    public Party getMe() {
        return me;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Collections.singletonList(me);
    }

    // Defines the scheduled activity to be conducted by the SchedulableState.
    @Nullable
    @Override
    public ScheduledActivity nextScheduledActivity(@NotNull StateRef thisStateRef, @NotNull FlowLogicRefFactory flowLogicRefFactory) {
        // A heartbeat will be emitted every second.
        // We get the time when the scheduled activity will occur in the constructor rather than in this method. This is
        // because calling Instant.now() in nextScheduledActivity returns the time at which the function is called, rather
        // than the time at which the state was created.
        return new ScheduledActivity(flowLogicRefFactory.create("net.corda.samples.heartbeat.flows.HeartbeatFlow", thisStateRef), nextActivityTime);
    }
}
