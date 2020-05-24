package com.pr.server.common.bo.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pr.server.common.bo.BusinessObject;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

public class DegreeDetailsBO implements BusinessObject {
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

    public void setDegreeName(String degreeName) {
        this.degreeName = degreeName;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }

    public String getPassingYear() {
        return passingYear;
    }

    public void setPassingYear(String passingYear) {
        this.passingYear = passingYear;
    }

    public String getPassingDivision() {
        return passingDivision;
    }

    public void setPassingDivision(String passingDivision) {
        this.passingDivision = passingDivision;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getSpecializationField() {
        return specializationField;
    }

    public void setSpecializationField(String specializationField) {
        this.specializationField = specializationField;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }
}
