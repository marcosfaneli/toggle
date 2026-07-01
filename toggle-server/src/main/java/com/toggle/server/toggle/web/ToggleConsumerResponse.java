package com.toggle.server.toggle.web;

import com.toggle.server.client.application.ToggleConsumerView;

public record ToggleConsumerResponse(
        String serviceName,
        String instanceId,
        String podName,
        String namespace,
        String callbackUrl,
        String status,
        String consumeMode) {

    public static ToggleConsumerResponse from(ToggleConsumerView view) {
        return new ToggleConsumerResponse(
                view.serviceName(),
                view.instanceId(),
                view.podName(),
                view.namespace(),
                view.callbackUrl(),
                view.status(),
                view.consumeMode());
    }
}
