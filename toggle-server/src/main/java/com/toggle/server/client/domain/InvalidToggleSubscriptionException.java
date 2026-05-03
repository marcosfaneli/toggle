package com.toggle.server.client.domain;

import java.util.List;

public class InvalidToggleSubscriptionException extends RuntimeException {

    public InvalidToggleSubscriptionException(List<String> unknownToggleNames) {
        super("Unknown toggle(s): %s".formatted(String.join(", ", unknownToggleNames)));
    }
}
