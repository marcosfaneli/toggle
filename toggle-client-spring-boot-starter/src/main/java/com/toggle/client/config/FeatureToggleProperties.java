package com.toggle.client.config;

import com.toggle.client.runtime.ConsumeMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "feature.toggles")
public record FeatureToggleProperties(
        @DefaultValue("true") boolean enabled,
        @NotBlank String serviceName,
        @NotBlank String instanceId,
        @NotBlank String podName,
        @NotBlank String namespace,
        @NotBlank String serverBaseUrl,
        String apiKey,
        @NotBlank String callbackBaseUrl,
        @DefaultValue("/internal/feature-toggles") @NotBlank String callbackPath,
        @DefaultValue("30000") @Min(1000) long heartbeatIntervalMs,
        @NotEmpty Map<String, ConsumeMode> consumed) {

    public String callbackUrl() {
        return withoutTrailingSlash(callbackBaseUrl) + withLeadingSlash(callbackPath);
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String withLeadingSlash(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }
}
