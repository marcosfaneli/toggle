package com.toggle.client.runtime;

import com.toggle.client.config.FeatureToggleProperties;
import com.toggle.client.infra.ToggleServerClient;

import java.util.Optional;

public class ToggleRuntimeService implements FeatureToggleClient {

    private final FeatureToggleProperties properties;
    private final ToggleCache toggleCache;
    private final ToggleServerClient serverClient;

    public ToggleRuntimeService(FeatureToggleProperties properties,
                                ToggleCache toggleCache,
                                ToggleServerClient serverClient) {
        this.properties = properties;
        this.toggleCache = toggleCache;
        this.serverClient = serverClient;
    }

    @Override
    public boolean isEnabled(String toggleName) {
        return resolve(toggleName).enabled();
    }

    @Override
    public Optional<ToggleValue> getValue(String toggleName) {
        return Optional.ofNullable(resolve(toggleName).value());
    }

    @Override
    public ToggleResolution resolve(String toggleName) {
        var mode = properties.consumed().get(toggleName);
        if (mode == null) {
            throw new IllegalArgumentException("toggle is not configured in consumed map: " + toggleName);
        }

        if (mode == ConsumeMode.LOCAL_CACHE) {
            return toggleCache.get(toggleName)
                    .map(snapshot -> ToggleResolution.fromSnapshot(snapshot, mode, "CACHE"))
                    .orElseGet(() -> ToggleResolution.missing(toggleName, mode, "CACHE"));
        }

        return serverClient.fetchToggleByName(toggleName)
                .map(snapshot -> {
                    toggleCache.putIfNewer(snapshot);
                    return ToggleResolution.fromSnapshot(snapshot, mode, "REMOTE");
                })
                .orElseGet(() -> ToggleResolution.missing(toggleName, mode, "REMOTE"));
    }
}
