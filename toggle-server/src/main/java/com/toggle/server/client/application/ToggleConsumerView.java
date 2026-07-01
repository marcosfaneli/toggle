package com.toggle.server.client.application;

public record ToggleConsumerView(
        String serviceName,
        String instanceId,
        String podName,
        String namespace,
        String callbackUrl,
        String status,
        String consumeMode) {
}
