package com.toggle.server.toggle.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValueRequest(
        @NotBlank @Pattern(regexp = "STRING|NUMBER", message = "must be STRING or NUMBER") String type,
        @NotBlank String raw) {}
