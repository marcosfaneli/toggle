package com.toggle.server.client.application;

import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.ClientSubscription;
import com.toggle.server.client.domain.ConsumeMode;
import com.toggle.server.client.domain.InvalidToggleSubscriptionException;
import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegisterClientUseCase {

        private static final Logger log = LoggerFactory.getLogger(RegisterClientUseCase.class);

    private final ClientPersistenceAdapter clientPersistenceAdapter;
    private final TogglePersistenceAdapter togglePersistenceAdapter;
    private final Clock clock;

    public RegisterClientUseCase(ClientPersistenceAdapter clientPersistenceAdapter,
                                 TogglePersistenceAdapter togglePersistenceAdapter,
                                 Clock appClock) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
        this.togglePersistenceAdapter = togglePersistenceAdapter;
        this.clock = appClock;
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
            log.info(
                    "event=client_register_invalid_subscription serviceName={} instanceId={} unknownToggles={}",
                    command.serviceName(),
                    command.instanceId(),
                    unknownNames);
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
                Instant.now(clock));

        var subscriptions = command.subscriptions().stream()
                .map(sub -> new ClientSubscription(
                        null,
                        null,
                        sub.toggleName(),
                        ConsumeMode.valueOf(sub.consumeMode()),
                        Instant.now(clock)))
                .toList();

        var saved = clientPersistenceAdapter.upsert(instance, subscriptions);

        log.info(
                "event=client_registered serviceName={} instanceId={} publicId={} namespace={} podName={}",
                saved.serviceName(),
                saved.instanceId(),
                saved.publicId(),
                saved.namespace(),
                saved.podName());

        return new RegisterClientResult(saved, foundToggles);
    }
}
