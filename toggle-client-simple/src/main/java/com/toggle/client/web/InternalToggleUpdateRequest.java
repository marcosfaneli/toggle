package com.toggle.client.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record InternalToggleUpdateRequest(
        String name,
        @NotNull Boolean enabled,
        @NotNull Long version,
        Instant updatedAt,
        @Valid ValueRequest value) {

    public record ValueRequest(String type, String raw) {
    }
}
