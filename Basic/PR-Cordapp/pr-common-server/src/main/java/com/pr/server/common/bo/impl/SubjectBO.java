package com.pr.server.common.bo.impl;

import com.pr.server.common.bo.BusinessObject;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

public class SubjectBO implements BusinessObject {
    private String subjectName;
    private Double marksObtained;
    private String isPassed;

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Double getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(Double marksObtained) {
        this.marksObtained = marksObtained;
    }

    public String getPassed() {
        return isPassed;
    }

    public void setPassed(String passed) {
        isPassed = passed;
    }

}
