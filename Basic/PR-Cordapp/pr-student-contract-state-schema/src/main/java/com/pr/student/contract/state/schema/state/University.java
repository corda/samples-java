package com.pr.student.contract.state.schema.state;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@CordaSerializable
public class University {
    private String universityName;
    private String address;
    private String universityType;
    private String contactNumber;

    @ConstructorForDeserialization
    public University(String universityName, String address, String universityType, String contactNumber) {
        this.universityName = universityName;
        this.address = address;
        this.universityType = universityType;
        this.contactNumber = contactNumber;
    }

    public String getUniversityName() {
        return universityName;
    }

    public String getAddress() {
        return address;
    }

    public String getUniversityType() {
        return universityType;
    }

    public String getContactNumber() {
        return contactNumber;
    }
}
