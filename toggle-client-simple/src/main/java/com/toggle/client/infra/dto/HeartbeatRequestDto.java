package com.toggle.client.infra.dto;

public record HeartbeatRequestDto(
        String serviceName,
        String instanceId) {
}
