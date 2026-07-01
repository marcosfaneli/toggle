package com.toggle.server.toggle.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record CreateToggleRequest(
        @NotBlank String name,
        @NotBlank String maintainer,
        boolean enabled,
        @Valid ValueRequest value) {
}
