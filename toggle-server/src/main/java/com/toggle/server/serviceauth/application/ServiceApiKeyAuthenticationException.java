package com.toggle.server.serviceauth.application;

public class ServiceApiKeyAuthenticationException extends RuntimeException {

    public ServiceApiKeyAuthenticationException() {
        super("Invalid service API key");
    }
}
