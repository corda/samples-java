package net.corda.samples.duediligence.contracts

import net.corda.core.identity.Party
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import org.junit.Test
import kotlin.test.assertEquals

class StateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the amount field exist?
        CorporateRecordsAuditRequest::class.java.getDeclaredField("applicant")
        // Is the amount field of the correct type?
        assertEquals(CorporateRecordsAuditRequest::class.java.getDeclaredField("applicant").type, Party::class.java)
    }
}