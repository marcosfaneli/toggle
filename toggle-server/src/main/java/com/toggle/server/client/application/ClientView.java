package com.toggle.server.client.application;

import java.time.Instant;
import java.util.List;

public record ClientView(
        String publicId,
        String serviceName,
        String instanceId,
        String podName,
        String namespace,
        String callbackUrl,
        String status,
        Instant registeredAt,
        List<SubscriptionView> subscriptions) {

    public record SubscriptionView(String toggleName, String consumeMode) {}
}
