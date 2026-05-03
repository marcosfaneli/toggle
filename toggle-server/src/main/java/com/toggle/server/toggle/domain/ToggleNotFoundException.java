package com.toggle.server.toggle.domain;

public class ToggleNotFoundException extends RuntimeException {

    public ToggleNotFoundException(String name, String ownerServiceName) {
        super("Toggle '%s' not found for service '%s'".formatted(name, ownerServiceName));
    }
}
