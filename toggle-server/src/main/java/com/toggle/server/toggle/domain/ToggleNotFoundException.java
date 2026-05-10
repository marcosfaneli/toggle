package com.toggle.server.toggle.domain;

public class ToggleNotFoundException extends RuntimeException {

    public ToggleNotFoundException(String name) {
        super("Toggle '%s' not found".formatted(name));
    }
}
