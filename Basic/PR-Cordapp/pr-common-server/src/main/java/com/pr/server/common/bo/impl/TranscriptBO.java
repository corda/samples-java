package com.pr.server.common.bo.impl;

import java.util.List;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

public class TranscriptBO {
    private String rollNumber;
    private String name;
    private String universityName;
    private String dateOfCompletion;
    private String degreeName;
    private Boolean isPass;
    private List<SemesterBO> semester;

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getDateOfCompletion() {
        return dateOfCompletion;
    }

    public void setDateOfCompletion(String dateOfCompletion) {
        this.dateOfCompletion = dateOfCompletion;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public void setDegreeName(String degreeName) {
        this.degreeName = degreeName;
    }

    public Boolean getPass() {
        return isPass;
    }

    public void setPass(Boolean pass) {
        isPass = pass;
    }

    public List<SemesterBO> getSemester() {
        return semester;
    }

    public void setSemester(List<SemesterBO> semester) {
        this.semester = semester;
    }
}
