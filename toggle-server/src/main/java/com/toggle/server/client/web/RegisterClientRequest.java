package com.toggle.server.client.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record RegisterClientRequest(
        @NotBlank String serviceName,
        @NotBlank String instanceId,
        @NotBlank String podName,
        @NotBlank String namespace,
        @NotBlank String callbackUrl,
        @NotEmpty @Valid List<SubscriptionRequest> subscriptions) {

    public record SubscriptionRequest(
            @NotBlank String toggleName,
            @NotBlank @Pattern(regexp = "LOCAL_CACHE|REMOTE_ALWAYS", message = "must be LOCAL_CACHE or REMOTE_ALWAYS") String consumeMode) {}
}
