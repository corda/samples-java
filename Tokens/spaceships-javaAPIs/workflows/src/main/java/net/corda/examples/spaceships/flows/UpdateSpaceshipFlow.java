package net.corda.examples.spaceships.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.spaceships.states.SpaceshipTokenType;

/**
 * UpdateSpaceShipFlow is for the maintainer/s of the EvolvableTokenType
 * to change the state definition. In this case changes can be made to the:
 * - seatingCapacity of the ship (if it's been upgraded or changed)
 * - value of the ship (based on current market or other)
 *
 * The subflow 'UpdateEvolvableToken' commits the changes and distributes the new definition
 * to all parties on the distribution list.
 */
@StartableByRPC
public class UpdateSpaceshipFlow extends FlowLogic<SignedTransaction> {

    private final StateAndRef<SpaceshipTokenType> shipRef;
    private final int seatingCapacity;
    private final Amount<TokenType> value;

    public UpdateSpaceshipFlow(StateAndRef<SpaceshipTokenType> shipRef, int seatingCapacity, Amount<TokenType> value) {
        this.shipRef = shipRef;
        this.seatingCapacity = seatingCapacity;
        this.value = value;
    }

    // Overload to update value only
    public UpdateSpaceshipFlow(StateAndRef<SpaceshipTokenType> shipRef, Amount<TokenType> value) {
        this(shipRef, shipRef.getState().getData().getSeatingCapacity(), value);
    }

    // Overload to update seating capacity only
    public UpdateSpaceshipFlow(StateAndRef<SpaceshipTokenType> shipRef, int seatingCapacity) {
        this(shipRef, seatingCapacity, shipRef.getState().getData().getValue());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        final SpaceshipTokenType ship = shipRef.getState().getData();
        final SpaceshipTokenType updatedShip = SpaceshipTokenType.createUpdatedSpaceShipTokenType(ship, seatingCapacity, value);

        return subFlow(new UpdateEvolvableToken(shipRef, updatedShip));
    }
}
