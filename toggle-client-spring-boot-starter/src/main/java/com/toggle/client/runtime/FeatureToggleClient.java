package com.toggle.client.runtime;

import java.util.Optional;

public interface FeatureToggleClient {

    boolean isEnabled(String toggleName);

    Optional<ToggleValue> getValue(String toggleName);

    ToggleResolution resolve(String toggleName);
}
