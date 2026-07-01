package com.toggle.client.infra;

import com.toggle.client.config.FeatureToggleProperties;
import com.toggle.client.infra.dto.HeartbeatRequestDto;
import com.toggle.client.infra.dto.RegisterClientRequestDto;
import com.toggle.client.infra.dto.RegisterClientResponseDto;
import com.toggle.client.infra.dto.ToggleResponseDto;
import com.toggle.client.runtime.ToggleSnapshot;
import com.toggle.client.runtime.ToggleValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class HttpToggleServerClient implements ToggleServerClient {

    private static final Logger log = LoggerFactory.getLogger(HttpToggleServerClient.class);

    private final RestClient restClient;
    private final FeatureToggleProperties properties;
    private volatile boolean registered;

    public HttpToggleServerClient(RestClient toggleServerRestClient, FeatureToggleProperties properties) {
        this.restClient = toggleServerRestClient;
        this.properties = properties;
    }

    @Override
    public List<ToggleSnapshot> registerAndGetSnapshot() {
        var validSubscriptions = properties.consumed().entrySet().stream()
                .map(entry -> new RegisterClientRequestDto.SubscriptionDto(entry.getKey(), entry.getValue().name()))
                .toList();

        if (validSubscriptions.isEmpty()) {
            registered = false;
            log.warn("Client registration skipped: consumed toggle configuration is empty.");
            return List.of();
        }

        var request = new RegisterClientRequestDto(
                properties.serviceName(),
                properties.instanceId(),
                properties.podName(),
                properties.namespace(),
                properties.callbackUrl(),
                validSubscriptions);

        RegisterClientResponseDto response;
        try {
            response = restClient.post()
                    .uri("/clients/register")
                    .body(request)
                    .retrieve()
                    .body(RegisterClientResponseDto.class);
        } catch (HttpClientErrorException.BadRequest ex) {
            registered = false;
            log.warn("Client registration rejected with 400: {}", ex.getResponseBodyAsString());
            return List.of();
        } catch (RestClientException ex) {
            registered = false;
            log.warn("Client registration failed: {}", ex.getMessage());
            return List.of();
        }

        if (response == null || response.toggles() == null) {
            registered = false;
            return List.of();
        }

        registered = true;

        return response.toggles().stream()
                .map(this::toSnapshot)
                .toList();
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public void heartbeat() {
        var request = new HeartbeatRequestDto(properties.serviceName(), properties.instanceId());
        restClient.post()
                .uri("/clients/heartbeat")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void deregister() {
        if (!registered) {
            return;
        }
        log.info("Deregistering client instance {} before shutdown", properties.instanceId());
        try {
            restClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/clients/register/{instanceId}")
                            .queryParam("serviceName", properties.serviceName())
                            .build(properties.instanceId()))
                    .retrieve()
                    .toBodilessEntity();
            registered = false;
        } catch (RestClientException ex) {
            log.warn("Failed to deregister client instance {}: {}", properties.instanceId(), ex.getMessage());
        }
    }

    @Override
    public Optional<ToggleSnapshot> fetchToggleByName(String toggleName) {
        try {
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/toggles/{name}")
                            .build(toggleName))
                    .retrieve()
                    .body(ToggleResponseDto.class);

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(toSnapshot(response));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch toggle by name {}: {}", toggleName, ex.getMessage());
            return Optional.empty();
        }
    }

    private ToggleSnapshot toSnapshot(RegisterClientResponseDto.ToggleEntryDto source) {
        return new ToggleSnapshot(
                source.name(),
                source.enabled(),
                source.value() == null ? null : new ToggleValue(source.value().type(), source.value().raw()),
                source.version(),
                source.updatedAt() == null ? Instant.now() : source.updatedAt());
    }

    private ToggleSnapshot toSnapshot(ToggleResponseDto source) {
        return new ToggleSnapshot(
                source.name(),
                source.enabled(),
                source.value() == null ? null : new ToggleValue(source.value().type(), source.value().raw()),
                source.version(),
                source.updatedAt() == null ? Instant.now() : source.updatedAt());
    }
}
