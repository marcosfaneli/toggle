package com.toggle.server.serviceauth.web;

import jakarta.validation.constraints.NotBlank;

public record CreateServiceApiKeyRequest(
        @NotBlank String serviceName,
        @NotBlank String name) {
}
