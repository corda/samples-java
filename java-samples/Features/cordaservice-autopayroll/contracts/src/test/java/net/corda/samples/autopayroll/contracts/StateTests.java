package net.corda.samples.autopayroll.contracts;
import net.corda.samples.autopayroll.states.MoneyState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        MoneyState.class.getDeclaredField("amount");
        // Is the message field of the correct type?
        assert(MoneyState.class.getDeclaredField("amount").getType().equals(int.class));
    }
}