package net.corda.samples.stockpaydividend.contracts;

import net.corda.samples.stockpaydividend.states.StockState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    //sample State tests
    @Test
    public void hasConstructionAreaFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        StockState.class.getDeclaredField("symbol");
        // Is the message field of the correct type?
        assert(StockState.class.getDeclaredField("symbol").getType().equals(String.class));
    }
}