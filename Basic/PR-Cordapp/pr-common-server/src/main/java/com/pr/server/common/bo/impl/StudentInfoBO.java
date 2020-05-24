package com.pr.server.common.bo.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pr.server.common.bo.BusinessObject;
import com.pr.server.common.deserializer.StudentInfoBODeserializer;
import net.corda.core.contracts.UniqueIdentifier;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@JsonDeserialize(using = StudentInfoBODeserializer.class)
public class StudentInfoBO implements BusinessObject {
    private String rollNumber;
    private String WESReferenceNumber;
    private String firstName;
    private String lastName;
    private String courseDuration;
    private String degreeStatus;
    private DegreeDetailsBO degreeDetailsBO;
    private TranscriptBO transcriptBO;
    private UniversityBO universityBO;
    private String status;

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getWESReferenceNumber() {
        return WESReferenceNumber;
    }

    public void setWESReferenceNumber(String WESReferenceNumber) {
        this.WESReferenceNumber = WESReferenceNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCourseDuration() {
        return courseDuration;
    }

    public void setCourseDuration(String courseDuration) {
        this.courseDuration = courseDuration;
    }

    public String getDegreeStatus() {
        return degreeStatus;
    }

    public void setDegreeStatus(String degreeStatus) {
        this.degreeStatus = degreeStatus;
    }

    public DegreeDetailsBO getDegreeDetailsBO() {
        return degreeDetailsBO;
    }

    public void setDegreeDetailsBO(DegreeDetailsBO degreeDetailsBO) {
        this.degreeDetailsBO = degreeDetailsBO;
    }

    public TranscriptBO getTranscriptBO() {
        return transcriptBO;
    }

    public void setTranscriptBO(TranscriptBO transcriptBO) {
        this.transcriptBO = transcriptBO;
    }

    public UniversityBO getUniversityBO() {
        return universityBO;
    }

    public void setUniversityBO(UniversityBO universityBO) {
        this.universityBO = universityBO;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
