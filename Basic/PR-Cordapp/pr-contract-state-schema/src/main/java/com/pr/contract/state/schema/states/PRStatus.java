package com.pr.contract.state.schema.states;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.corda.core.serialization.CordaSerializable;

import java.util.Arrays;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@CordaSerializable
public enum PRStatus {

    ACCOUNT_CREATED("ACCOUNT_CREATED"),
    APPLICATION_SUBMITTED("APPLICATION_SUBMITTED"),
    APPLICATION_ACKNOWLEDGEMENT("APPLICATION_ACKNOWLEDGEMENT"),
    DOCUMENT_RECEIVED("DOCUMENT_RECEIVED"),
    DOCUMENT_REVIEWED("DOCUMENT_REVIEWED"),
    ECA_REPORT_CREATED("ECA_REPORT_CREATED");

    private String textValue;

    PRStatus(String textValue) {
        this.textValue = textValue;
    }

    @JsonCreator
    public static PRStatus fromText(String input) {
        for (PRStatus value : PRStatus.values()) {
            if (value.getTxtValue().equalsIgnoreCase(input)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unable to find equivalent PR Application Status, try only the following" + Arrays.toString(PRStatus.values()));
    }

    public String getTxtValue() {
        return textValue;
    }

    @Override
    public String toString() {
        return textValue;
    }

}
