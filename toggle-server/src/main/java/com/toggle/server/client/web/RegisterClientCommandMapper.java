package com.toggle.server.client.web;

import com.toggle.server.client.application.RegisterClientCommand;
import org.springframework.stereotype.Component;

@Component
class RegisterClientCommandMapper {

    RegisterClientCommand toCommand(RegisterClientRequest request) {
        var subscriptions = request.subscriptions().stream()
                .map(sub -> new RegisterClientCommand.SubscriptionCommand(sub.toggleName(), sub.consumeMode()))
                .toList();

        return new RegisterClientCommand(
                request.serviceName(),
                request.instanceId(),
                request.podName(),
                request.namespace(),
                request.callbackUrl(),
                subscriptions);
    }
}
