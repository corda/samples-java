package com.pr.student.contract.state.schema.state;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@CordaSerializable
public class Subjects {
    private String subjectName;
    private Double marksObtained;
    private String isPassed;

    @ConstructorForDeserialization
    public Subjects(String subjectName, Double marksObtained) {
        this.subjectName = subjectName;
        this.marksObtained = marksObtained;
        //this.isPassed = isPassed;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public Double getMarksObtained() {
        return marksObtained;
    }

    public String getPassed() {
        return isPassed;
    }

    @Override
    public String toString() {
        return "Subjects{" +
                "subjectName='" + subjectName + '\'' +
                ", marksObtained=" + marksObtained +
                '}';
    }
}
