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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
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
        validateCallbackUrl(command.callbackUrl(), command.serviceName(), command.instanceId());

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

    private void validateCallbackUrl(String callbackUrl, String serviceName, String instanceId)
            throws InvalidCallbackUrlException {
        // Validate non-null and non-blank
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw rejectedCallback(serviceName, instanceId,
                    "Callback URL cannot be null or blank", "n/a", "n/a");
        }

        try {
            URI uri = new URI(callbackUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null ||
                    !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw rejectedCallback(serviceName, instanceId,
                        "Callback URL must use http or https", hostOrUnknown(host), schemeOrUnknown(scheme));
            }

            if (uri.getUserInfo() != null) {
                throw rejectedCallback(serviceName, instanceId,
                        "Callback URL must not contain user info", hostOrUnknown(host), schemeOrUnknown(scheme));
            }

            if (uri.getFragment() != null) {
                throw rejectedCallback(serviceName, instanceId,
                        "Callback URL must not contain fragment", hostOrUnknown(host), schemeOrUnknown(scheme));
            }

            if (host == null || host.isEmpty()) {
                throw rejectedCallback(serviceName, instanceId,
                        "Callback URL has no host", "n/a", schemeOrUnknown(scheme));
            }

            // Block internal/reserved addresses to prevent SSRF attacks (unless explicitly allowed)
            if (!allowLocalCallbacks) {
                var normalizedHost = host.toLowerCase(Locale.ROOT);
                if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")) {
                    throw rejectedCallback(serviceName, instanceId,
                            "Callback URL points to reserved or internal network: " + host,
                            normalizedHost, schemeOrUnknown(scheme));
                }

                if (isIpLiteral(normalizedHost)) {
                    if (isReservedAddress(normalizedHost)) {
                        throw rejectedCallback(serviceName, instanceId,
                                "Callback URL points to reserved or internal network: " + host,
                                normalizedHost, schemeOrUnknown(scheme));
                    }
                } else {
                    if (hostnameResolvesToReservedAddress(normalizedHost)) {
                        throw rejectedCallback(serviceName, instanceId,
                                "Callback URL resolves to reserved or internal network: " + host,
                                normalizedHost, schemeOrUnknown(scheme));
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw rejectedCallback(serviceName, instanceId,
                    "Invalid callback URL format: " + e.getMessage(), "n/a", "n/a");
        }
    }

    private InvalidCallbackUrlException rejectedCallback(String serviceName,
                                                         String instanceId,
                                                         String reason,
                                                         String host,
                                                         String scheme) {
        log.warn(
                "event=client_register_callback_rejected serviceName={} instanceId={} reason={} scheme={} host={} allowLocalCallbacks={}",
                serviceName,
                instanceId,
                reason,
                scheme,
                host,
                allowLocalCallbacks);
        return new InvalidCallbackUrlException(reason);
    }

    private String schemeOrUnknown(String scheme) {
        return scheme == null || scheme.isBlank() ? "unknown" : scheme;
    }

    private String hostOrUnknown(String host) {
        return host == null || host.isBlank() ? "unknown" : host;
    }

    private boolean hostnameResolvesToReservedAddress(String hostname) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            for (InetAddress address : addresses) {
                if (isReservedInetAddress(address)) {
                    return true;
                }
            }
            return false;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean isReservedInetAddress(InetAddress address) {
        if (address.isAnyLocalAddress() ||
                address.isLoopbackAddress() ||
                address.isLinkLocalAddress() ||
                address.isSiteLocalAddress() ||
                address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            return first == 100 && second >= 64 && second <= 127;
        }

        if (bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            return (first & 0xFE) == 0xFC;
        }
        return false;
    }

    private boolean isIpLiteral(String host) {
        if (host.contains(":")) {
            return true;
        }

        // Fast IPv4 literal check to avoid DNS lookups for hostnames.
        return host.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    }

    private boolean isReservedAddress(String ipLiteral) {
        try {
            InetAddress address = InetAddress.getByName(ipLiteral);
            byte[] bytes = address.getAddress();

            // Blocks loopback, link-local, site-local and unspecified addresses.
            if (address.isAnyLocalAddress() ||
                    address.isLoopbackAddress() ||
                    address.isLinkLocalAddress() ||
                    address.isSiteLocalAddress() ||
                    address.isMulticastAddress()) {
                return true;
            }

            // Block CGNAT range 100.64.0.0/10.
            if (bytes.length == 4) {
                int first = bytes[0] & 0xFF;
                int second = bytes[1] & 0xFF;
                return first == 100 && second >= 64 && second <= 127;
            }

            // Block IPv6 Unique Local Address range fc00::/7.
            if (bytes.length == 16) {
                int first = bytes[0] & 0xFF;
                return (first & 0xFE) == 0xFC;
            }
            return false;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
