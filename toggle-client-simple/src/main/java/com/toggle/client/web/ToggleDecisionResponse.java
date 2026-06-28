package com.toggle.client.web;

public record ToggleDecisionResponse(
        String toggleName,
        boolean enabled,
        String strategy,
        String message) {
}
