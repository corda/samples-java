package net.corda.samples.supplychain.contracts;

import net.corda.samples.supplychain.states.InvoiceState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        InvoiceState.class.getDeclaredField("amount");
        // Is the message field of the correct type?
        assert(InvoiceState.class.getDeclaredField("amount").getType().equals(int.class));
    }
}