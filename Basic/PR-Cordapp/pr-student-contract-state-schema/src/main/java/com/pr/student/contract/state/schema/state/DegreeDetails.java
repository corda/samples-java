package com.pr.student.contract.state.schema.state;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@CordaSerializable
public class DegreeDetails {
    private String degreeName;
    private String universityName;
    private String completionStatus;
    private String passingYear;
    private String passingDivision;
    private String fullName;
    private String fatherName;
    private String specializationField;
    private String rollNumber;

    public String getDegreeName() {
        return degreeName;
    }

    public String getUniversityName() {
        return universityName;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public String getPassingYear() {
        return passingYear;
    }

    public String getPassingDivision() {
        return passingDivision;
    }

    public String getFullName() {
        return fullName;
    }

    public String getFatherName() {
        return fatherName;
    }

    public String getSpecializationField() {
        return specializationField;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    @ConstructorForDeserialization
    public DegreeDetails(String degreeName, String universityName, String completionStatus, String passingYear,
                         String passingDivision, String fullName, String fatherName, String specializationField,
                         String rollNumber) {
        this.degreeName = degreeName;
        this.universityName = universityName;
        this.completionStatus = completionStatus;
        this.passingYear = passingYear;
        this.passingDivision = passingDivision;
        this.fullName = fullName;
        this.fatherName = fatherName;
        this.specializationField = specializationField;
        this.rollNumber = rollNumber;
    }

    public DegreeDetails(DegreeDetails other) {
        this.degreeName = other.degreeName;
        this.universityName = other.universityName;
        this.completionStatus = other.completionStatus;
        this.passingYear = other.passingYear;
        this.passingDivision = other.passingDivision;
        this.fullName = other.fullName;
        this.fatherName = other.fatherName;
        this.specializationField = other.specializationField;
        this.rollNumber = other.rollNumber;
    }
}
