package com.toggle.server.toggle.web;

import jakarta.validation.constraints.NotBlank;

public record CreateToggleRequest(
        @NotBlank String name,
        @NotBlank String ownerServiceName,
        boolean enabled) {
}
