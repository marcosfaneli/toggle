package com.toggle.server.client.application;

import java.util.List;

public record RegisterClientCommand(
        String serviceName,
        String instanceId,
        String podName,
        String namespace,
        String callbackUrl,
        List<SubscriptionCommand> subscriptions) {

    public record SubscriptionCommand(String toggleName, String consumeMode) {}
}
