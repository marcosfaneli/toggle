package com.toggle.server.serviceauth.domain;

public class ServiceApiKeyNotFoundException extends RuntimeException {

    public ServiceApiKeyNotFoundException(String publicId) {
        super("Service API key not found: " + publicId);
    }
}
