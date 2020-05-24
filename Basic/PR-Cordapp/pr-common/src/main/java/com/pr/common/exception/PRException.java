package com.pr.common.exception;

import net.corda.core.serialization.CordaSerializable;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@CordaSerializable
public class PRException extends RuntimeException {
    public PRException() {
    }

    public PRException(String message) {
        super(message);
    }

    public PRException(String message, Throwable cause) {
        super(message, cause);
    }

    public PRException(Throwable cause) {
        super(cause);
    }

    public PRException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}