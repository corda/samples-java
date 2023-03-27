package com.tutorial.contracts

import com.tutorial.states.AppleStamp
import com.tutorial.states.TemplateState
import net.corda.core.identity.Party
import org.junit.Test
import kotlin.test.assertEquals

class StateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        TemplateState::class.java.getDeclaredField("msg")
        // Is the field of the correct type?
        assertEquals(TemplateState::class.java.getDeclaredField("msg").type, String()::class.java)
    }

    @Test
    fun AppleStampStateHasFieldOfCorrectType(){
        AppleStamp::class.java.getDeclaredField("stampDesc")
        assert(AppleStamp::class.java.getDeclaredField("stampDesc").type == String::class.java)

        AppleStamp::class.java.getDeclaredField("issuer")
        assert(AppleStamp::class.java.getDeclaredField("issuer").type == Party::class.java)

        AppleStamp::class.java.getDeclaredField("holder")
        assert(AppleStamp::class.java.getDeclaredField("issuer").type == Party::class.java)
    }

}