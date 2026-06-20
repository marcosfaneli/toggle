package com.toggle.server.client.domain;

public class InvalidCallbackUrlException extends RuntimeException {
    public InvalidCallbackUrlException(String message) {
        super(message);
    }

    public InvalidCallbackUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
