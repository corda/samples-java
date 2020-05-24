package com.pr.student.contract.state.schema.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.corda.core.serialization.CordaSerializable;

import java.util.Arrays;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@CordaSerializable
public enum RequestStatus {

    APPLICATION_SUBMITTED("APPLICATION_SUBMITTED"),
    PAYMENT_RECEIVED_ACKNOWLEDGEMENT("PAYMENT_RECEIVED_ACKNOWLEDGEMENT"),
    CONFIRMED("CONFIRMED"),
    REJECT("REJECT"),
    APPLICATION_READY_FOR_WES_VERIFICATION("APPLICATION_READY_FOR_WES_VERIFICATION"),
    ADDED_TRANSCRIPT_DETAILS("ADDED_TRANSCRIPT_DETAILS");

    private String textValue;

    RequestStatus(String textValue) {
        this.textValue = textValue;
    }

    @JsonCreator
    public static RequestStatus fromText(String input) {
        for (RequestStatus value : RequestStatus.values()) {
            if (value.getTxtValue().equalsIgnoreCase(input)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unable to find equivalent Request Application Status, try only the following" + Arrays.toString(RequestStatus.values()));
    }

    public String getTxtValue() {
        return textValue;
    }

    @Override
    public String toString() {
        return textValue;
    }

}

