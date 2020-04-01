package net.corda.samples.states;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;

import java.util.*;
import com.google.common.collect.ImmutableList;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.contracts.IOUContract;

/**
 * The IOU State object, with the following properties:
 * - [amount] The amount owed by the [borrower] to the [lender]
 * - [lender] The lending party.
 * - [borrower] The borrowing party.
 * - [contract] Holds a reference to the [IOUContract]
 * - [paid] Records how much of the [amount] has been paid.
 * - [linearId] A unique id shared by all LinearState states representing the same agreement throughout history within
 *   the vaults of all parties. Verify methods should check that one input and one output share the id in a transaction,
 *   except at issuance/termination.
 */

@BelongsToContract(IOUContract.class)
public class IOUState implements ContractState, LinearState {

    public final Amount<Currency> amount;
    public final Party lender;
    public final Party borrower;
    public final Amount<Currency> paid;
    private final UniqueIdentifier linearId;

    // Private constructor used only for copying a State object
    @ConstructorForDeserialization
    private IOUState(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid, UniqueIdentifier linearId){
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
    }

    public IOUState(Amount<Currency> amount, Party lender, Party borrower) {
        this(amount, lender, borrower, new Amount<>(0, amount.getToken()), new UniqueIdentifier());
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public Amount<Currency> getPaid() {
        return paid;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    /**
     *  This method will return a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewLender] creates a copy of the current state with a newly specified lender. For use when transferring.
     * - [copy] creates a copy of the state using the internal copy constructor ensuring the LinearId is preserved.
     */
    public IOUState pay(Amount<Currency> amountToPay) {
        Amount<Currency> newAmountPaid = this.paid.plus(amountToPay);
        return new IOUState(amount, lender, borrower, newAmountPaid, linearId);
    }

    public IOUState withNewLender(Party newLender) {
        return new IOUState(amount, newLender, borrower, paid, linearId);
    }

    public IOUState copy(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid) {
        return new IOUState(amount, lender, borrower, paid, this.getLinearId());
    }

}