package com.toggle.client.example.web;

public record CheckoutDecisionResponse(
        String toggleName,
        boolean enabled,
        String strategy,
        ToggleValueResponse value) {

    public record ToggleValueResponse(String type, String raw) {
    }
}
