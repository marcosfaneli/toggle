package com.toggle.server.client.domain;

import java.time.Instant;

public record ClientInstance(
        String publicId,
        String serviceName,
        String instanceId,
        String podName,
        String namespace,
        String callbackUrl,
        ClientInstanceStatus status,
        Instant registeredAt) {
}
