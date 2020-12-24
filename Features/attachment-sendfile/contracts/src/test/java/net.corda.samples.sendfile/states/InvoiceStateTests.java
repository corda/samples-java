package net.corda.samples.sendfile.states;

import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class InvoiceStateTests {

    private final Party a = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party b = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();

    private final String STRINGID = "StringID that is unique";

    @Test
    public void constructorTest() {
        InvoiceState st = new InvoiceState(STRINGID, Arrays.asList(a, b));
        assertEquals(STRINGID, st.getInvoiceAttachmentID());
    }


}
