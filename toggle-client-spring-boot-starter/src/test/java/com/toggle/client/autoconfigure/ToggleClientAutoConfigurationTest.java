package com.toggle.client.autoconfigure;

import com.toggle.client.runtime.FeatureToggleClient;
import com.toggle.client.web.FeatureToggleCallbackController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ToggleClientAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ToggleClientAutoConfiguration.class))
            .withPropertyValues(
                    "feature.toggles.service-name=checkout-service",
                    "feature.toggles.instance-id=checkout-local-1",
                    "feature.toggles.pod-name=checkout-local-1",
                    "feature.toggles.namespace=local",
                    "feature.toggles.server-base-url=http://localhost:8080",
                    "feature.toggles.callback-base-url=http://localhost:8082",
                    "feature.toggles.consumed.new-checkout=LOCAL_CACHE");

    @Test
    void shouldCreateClientBeansWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FeatureToggleClient.class);
            assertThat(context).hasSingleBean(FeatureToggleCallbackController.class);
        });
    }

    @Test
    void shouldBackOffWhenDisabled() {
        contextRunner
                .withPropertyValues("feature.toggles.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FeatureToggleClient.class);
                    assertThat(context).doesNotHaveBean(FeatureToggleCallbackController.class);
                });
    }
}
