package com.pr.student.contract.state.schema.state;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.util.List;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */
@CordaSerializable
public class Semester {
    private String semesterNumber;
    private List<Subjects> subbjectsList;
    private String resultDeclaredOnDate;

    @ConstructorForDeserialization
    public Semester(String semesterNumber, List<Subjects> subbjectsList, String resultDeclaredOnDate) {
        this.semesterNumber = semesterNumber;
        this.subbjectsList = subbjectsList;
        this.resultDeclaredOnDate = resultDeclaredOnDate;
    }

    public String getSemesterNumber() {
        return semesterNumber;
    }

    public List<Subjects> getSubbjectsList() {
        return subbjectsList;
    }

    public String getResultDeclaredOnDate() {
        return resultDeclaredOnDate;
    }
}
