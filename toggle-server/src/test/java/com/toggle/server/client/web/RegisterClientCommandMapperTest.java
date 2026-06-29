package com.toggle.server.client.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterClientCommandMapperTest {

    private final RegisterClientCommandMapper mapper = new RegisterClientCommandMapper();

    @Test
    void shouldMapRequestToCommand() {
        var subscriptions = List.of(
                new RegisterClientRequest.SubscriptionRequest("new-checkout", "LOCAL_CACHE"),
                new RegisterClientRequest.SubscriptionRequest("payment-v2", "REMOTE_ALWAYS"));

        var request = new RegisterClientRequest(
                "checkout-service",
                "checkout-7d8d4c7f6f-abcde",
                "checkout-7d8d4c7f6f-abcde",
                "payments",
                "http://10.42.1.25:8080/internal/feature-toggles",
                subscriptions);

        var command = mapper.toCommand(request);

        assertThat(command.serviceName()).isEqualTo("checkout-service");
        assertThat(command.instanceId()).isEqualTo("checkout-7d8d4c7f6f-abcde");
        assertThat(command.podName()).isEqualTo("checkout-7d8d4c7f6f-abcde");
        assertThat(command.namespace()).isEqualTo("payments");
        assertThat(command.callbackUrl()).isEqualTo("http://10.42.1.25:8080/internal/feature-toggles");
        assertThat(command.subscriptions()).hasSize(2);
        assertThat(command.subscriptions().get(0).toggleName()).isEqualTo("new-checkout");
        assertThat(command.subscriptions().get(0).consumeMode()).isEqualTo("LOCAL_CACHE");
        assertThat(command.subscriptions().get(1).toggleName()).isEqualTo("payment-v2");
        assertThat(command.subscriptions().get(1).consumeMode()).isEqualTo("REMOTE_ALWAYS");
    }

    @Test
    void shouldMapEmptySubscriptionsToEmptyList() {
        var request = new RegisterClientRequest(
                "svc",
                "inst-1",
                "pod-1",
                "ns",
                "http://host/cb",
                List.of());

        var command = mapper.toCommand(request);

        assertThat(command.subscriptions()).isEmpty();
    }
}
