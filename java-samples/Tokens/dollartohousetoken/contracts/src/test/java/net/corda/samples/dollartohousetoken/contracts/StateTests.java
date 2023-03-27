package net.corda.samples.dollartohousetoken.contracts;


import net.corda.samples.dollartohousetoken.states.HouseState;
import net.corda.testing.node.MockServices;
import org.junit.Test;
public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    //sample State tests
    @Test
    public void hasConstructionAreaFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        HouseState.class.getDeclaredField("constructionArea");
        // Is the message field of the correct type?
        assert(HouseState.class.getDeclaredField("constructionArea").getType().equals(String.class));
    }
}