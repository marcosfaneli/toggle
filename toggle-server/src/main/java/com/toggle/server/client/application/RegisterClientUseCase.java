package com.toggle.server.client.application;

import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.ClientSubscription;
import com.toggle.server.client.domain.ConsumeMode;
import com.toggle.server.client.domain.InvalidCallbackUrlException;
import com.toggle.server.client.domain.InvalidToggleSubscriptionException;
import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
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

        if (command.callbackUrl() != null && !command.callbackUrl().isBlank()) {
            validateCallbackUrl(command.callbackUrl());
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

    private void validateCallbackUrl(String callbackUrl) throws InvalidCallbackUrlException {
        try {
            URL url = new URL(callbackUrl);
            String host = url.getHost();
            
            if (host == null || host.isEmpty()) {
                throw new InvalidCallbackUrlException("Callback URL has no host");
            }
            
            // Block internal/reserved addresses to prevent SSRF attacks
            if (host.equals("localhost") ||
                host.equals("127.0.0.1") ||
                host.startsWith("169.254") ||    // AWS metadata
                host.startsWith("192.168") ||    // Private network
                host.startsWith("10.")) {        // Private network
                throw new InvalidCallbackUrlException(
                    "Callback URL points to reserved or internal network: " + host);
            }
        } catch (MalformedURLException e) {
            throw new InvalidCallbackUrlException("Invalid callback URL format: " + e.getMessage());
        }
    }
}
