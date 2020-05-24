package com.pr.server.common.bo.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pr.server.common.bo.BusinessObject;
import com.pr.server.common.deserializer.RequestBODeserializer;
import net.corda.core.serialization.CordaSerializable;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@CordaSerializable
@JsonDeserialize(using = RequestBODeserializer.class)
public class RequestFormBO implements BusinessObject {

    private Boolean isWESRequested;
    private Boolean isTranscriptRequested;
    private String WESReferenceNumber;
    private String universityName;
    private String studentName;
    private String rollNumber;
    private String degreeName;
    private String duration;
    private String universityAddress;
    private String WESAddress;
    private Boolean isApproved;
    private String comments;
    //private StudentInfoBO studentInfoBO;
    private String wesParty;
    private String universityParty;
    private String consultantParty;

    public String getWesParty() {
        return wesParty;
    }

    public void setWesParty(String wesParty) {
        this.wesParty = wesParty;
    }

    public String getUniversityParty() {
        return universityParty;
    }

    public void setUniversityParty(String universityParty) {
        this.universityParty = universityParty;
    }

    public String getConsultantParty() {
        return consultantParty;
    }

    public void setConsultantParty(String consultantParty) {
        this.consultantParty = consultantParty;
    }

   /* public StudentInfoBO getStudentInfoBO() {
        return studentInfoBO;
    }

    public void setStudentInfoBO(StudentInfoBO studentInfoBO) {
        this.studentInfoBO = studentInfoBO;
    }
*/
    public Boolean getWESRequested() {
        return isWESRequested;
    }

    public void setWESRequested(Boolean WESRequested) {
        isWESRequested = WESRequested;
    }

    public Boolean getTranscriptRequested() {
        return isTranscriptRequested;
    }

    public void setTranscriptRequested(Boolean transcriptRequested) {
        isTranscriptRequested = transcriptRequested;
    }

    public String getWESReferenceNumber() {
        return WESReferenceNumber;
    }

    public void setWESReferenceNumber(String WESReferenceNumber) {
        this.WESReferenceNumber = WESReferenceNumber;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public void setDegreeName(String degreeName) {
        this.degreeName = degreeName;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getUniversityAddress() {
        return universityAddress;
    }

    public void setUniversityAddress(String universityAddress) {
        this.universityAddress = universityAddress;
    }

    public String getWESAddress() {
        return WESAddress;
    }

    public void setWESAddress(String WESAddress) {
        this.WESAddress = WESAddress;
    }

    public Boolean getApproved() {
        return isApproved;
    }

    public void setApproved(Boolean approved) {
        isApproved = approved;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
