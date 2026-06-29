package com.toggle.client.infra.dto;

import java.util.List;

public record RegisterClientRequestDto(
        String serviceName,
        String instanceId,
        String podName,
        String namespace,
        String callbackUrl,
        List<SubscriptionDto> subscriptions) {

    public record SubscriptionDto(String toggleName, String consumeMode) {
    }
}
