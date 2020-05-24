package com.pr.server.common.exception;


import net.corda.core.serialization.CordaSerializable;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@CordaSerializable
public class PRServerException extends RuntimeException {
    public PRServerException() {
    }

    public PRServerException(String message) {
        super(message);
    }

    public PRServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public PRServerException(Throwable cause) {
        super(cause);
    }

    public PRServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
