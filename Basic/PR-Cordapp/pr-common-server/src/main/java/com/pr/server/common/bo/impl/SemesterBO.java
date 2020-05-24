package com.pr.server.common.bo.impl;

import com.pr.server.common.bo.BusinessObject;

import java.util.List;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

public class SemesterBO implements BusinessObject {
    private String semesterNumber;
    private List<SubjectBO> subbjectsList;
    private String resultDeclaredOnDate;

    public String getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(String semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public List<SubjectBO> getSubbjectsList() {
        return subbjectsList;
    }

    public void setSubbjectsList(List<SubjectBO> subbjectsList) {
        this.subbjectsList = subbjectsList;
    }

    public String getResultDeclaredOnDate() {
        return resultDeclaredOnDate;
    }

    public void setResultDeclaredOnDate(String resultDeclaredOnDate) {
        this.resultDeclaredOnDate = resultDeclaredOnDate;
    }
}
