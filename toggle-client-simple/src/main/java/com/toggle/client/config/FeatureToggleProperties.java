package com.toggle.client.config;

import com.toggle.client.runtime.ConsumeMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "feature.toggles")
public record FeatureToggleProperties(
        @NotBlank String serviceName,
        @NotBlank String instanceId,
        @NotBlank String podName,
        @NotBlank String namespace,
        @NotBlank String serverBaseUrl,
        @NotBlank String callbackBaseUrl,
        @NotBlank String callbackPath,
        @Min(1000) long heartbeatIntervalMs,
        @NotEmpty Map<String, ConsumeMode> consumed) {

    public String callbackUrl() {
        return callbackBaseUrl + callbackPath;
    }
}
