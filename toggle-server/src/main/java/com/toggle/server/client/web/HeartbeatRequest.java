package com.toggle.server.client.web;

import jakarta.validation.constraints.NotBlank;

public record HeartbeatRequest(
        @NotBlank String serviceName,
        @NotBlank String instanceId) {
}
