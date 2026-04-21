package com.toggle.server.toggle.domain;

public class ToggleAlreadyExistsException extends RuntimeException {

    public ToggleAlreadyExistsException(String name, String ownerServiceName) {
        super("Toggle '%s' already exists for service '%s'".formatted(name, ownerServiceName));
    }
}
