package com.toggle.server.client.application;

import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.ClientSubscription;
import com.toggle.server.client.domain.ConsumeMode;
import com.toggle.server.client.domain.InvalidToggleSubscriptionException;
import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegisterClientUseCase {

    private final ClientPersistenceAdapter clientPersistenceAdapter;
    private final TogglePersistenceAdapter togglePersistenceAdapter;

    public RegisterClientUseCase(ClientPersistenceAdapter clientPersistenceAdapter,
                                 TogglePersistenceAdapter togglePersistenceAdapter) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
        this.togglePersistenceAdapter = togglePersistenceAdapter;
    }

    public RegisterClientResult execute(RegisterClientCommand command) {
        var requestedNames = command.subscriptions().stream()
                .map(RegisterClientCommand.SubscriptionCommand::toggleName)
                .toList();

        var foundToggles = togglePersistenceAdapter.findAllByNames(requestedNames);

        Set<String> foundNames = foundToggles.stream()
                .map(t -> t.name())
                .collect(Collectors.toSet());

        var unknownNames = requestedNames.stream()
                .filter(name -> !foundNames.contains(name))
                .toList();

        if (!unknownNames.isEmpty()) {
            throw new InvalidToggleSubscriptionException(unknownNames);
        }

        var instance = new ClientInstance(
                null,
                command.serviceName(),
                command.instanceId(),
                command.podName(),
                command.namespace(),
                command.callbackUrl(),
                ClientInstanceStatus.ACTIVE,
                LocalDateTime.now());

        var subscriptions = command.subscriptions().stream()
                .map(sub -> new ClientSubscription(
                        null,
                        null,
                        sub.toggleName(),
                        ConsumeMode.valueOf(sub.consumeMode()),
                        LocalDateTime.now()))
                .toList();

        var saved = clientPersistenceAdapter.upsert(instance, subscriptions);

        return new RegisterClientResult(saved, foundToggles);
    }
}
