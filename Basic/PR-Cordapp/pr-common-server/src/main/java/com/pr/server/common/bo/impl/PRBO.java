package com.pr.server.common.bo.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pr.contract.state.schema.states.PRStatus;
import com.pr.server.common.bo.BusinessObject;
import com.pr.server.common.deserializer.PRBODeserializer;
import kotlinx.html.B;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.time.LocalDateTime;


/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@JsonSerialize
@CordaSerializable
@JsonDeserialize(using = PRBODeserializer.class)
public class PRBO implements BusinessObject {


    private String firstName;
    private String lastName;
    private String courseName;
    private String courseDuration;
    private String university;
    private String email;
    private String prStatus;
    private String consultantParty;
    private String wesParty;
    private Integer amount;


    @ConstructorForDeserialization
    public PRBO(String firstName, String lastName, String courseName, String courseDuration, String university, String email, String prStatus, String consultantParty, String wesParty, Integer amount) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.courseName = courseName;
        this.courseDuration = courseDuration;
        this.university = university;
        this.email = email;
        this.prStatus = prStatus;
        this.consultantParty = consultantParty;
        this.wesParty = wesParty;
        this.amount = amount;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseDuration() {
        return courseDuration;
    }

    public void setCourseDuration(String courseDuration) {
        this.courseDuration = courseDuration;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPrStatus() {
        return prStatus;
    }

    public void setPrStatus(String prStatus) {
        this.prStatus = prStatus;
    }

    public String getConsultantParty() {
        return consultantParty;
    }

    public void setConsultantParty(String consultantParty) {
        this.consultantParty = consultantParty;
    }

    public String getWesParty() {
        return wesParty;
    }

    public void setWesParty(String wesParty) {
        this.wesParty = wesParty;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }


}
