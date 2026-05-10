package com.toggle.server.toggle.domain;

public class ToggleAlreadyExistsException extends RuntimeException {

    public ToggleAlreadyExistsException(String name) {
        super("Toggle '%s' already exists".formatted(name));
    }
}
