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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
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
    private final boolean allowLocalCallbacks;

    public RegisterClientUseCase(ClientPersistenceAdapter clientPersistenceAdapter,
                                 TogglePersistenceAdapter togglePersistenceAdapter,
                                 Clock appClock,
                                 @Value("${toggle.security.allow-local-callbacks:false}") boolean allowLocalCallbacks) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
        this.togglePersistenceAdapter = togglePersistenceAdapter;
        this.clock = appClock;
        this.allowLocalCallbacks = allowLocalCallbacks;
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

        // Validate callback URL (including null/blank check)
        validateCallbackUrl(command.callbackUrl());

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
        // Validate non-null and non-blank
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new InvalidCallbackUrlException("Callback URL cannot be null or blank");
        }

        try {
            URI uri = new URI(callbackUrl);
            String host = uri.getHost();
            
            if (host == null || host.isEmpty()) {
                throw new InvalidCallbackUrlException("Callback URL has no host");
            }
            
            // Block internal/reserved addresses to prevent SSRF attacks (unless explicitly allowed)
            if (!allowLocalCallbacks && (
                host.equals("localhost") ||
                host.equals("127.0.0.1") ||
                host.startsWith("169.254") ||    // AWS metadata
                host.startsWith("192.168") ||    // Private network
                host.startsWith("10."))) {       // Private network
                throw new InvalidCallbackUrlException(
                    "Callback URL points to reserved or internal network: " + host);
            }
        } catch (URISyntaxException e) {
            throw new InvalidCallbackUrlException("Invalid callback URL format: " + e.getMessage());
        }
    }
}
