package net.corda.samples.duediligence.contracts;

import net.corda.core.identity.Party;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        CorporateRecordsAuditRequest.class.getDeclaredField("applicant");
        // Is the message field of the correct type?
        assert(CorporateRecordsAuditRequest.class.getDeclaredField("applicant").getType().equals(Party.class));
    }
}