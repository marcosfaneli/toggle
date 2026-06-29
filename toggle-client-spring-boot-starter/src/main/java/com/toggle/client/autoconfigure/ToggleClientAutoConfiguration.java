package com.toggle.client.autoconfigure;

import com.toggle.client.config.FeatureToggleProperties;
import com.toggle.client.infra.HttpToggleServerClient;
import com.toggle.client.infra.ToggleServerClient;
import com.toggle.client.runtime.ClientLifecycleService;
import com.toggle.client.runtime.FeatureToggleClient;
import com.toggle.client.runtime.ToggleCache;
import com.toggle.client.runtime.ToggleRuntimeService;
import com.toggle.client.web.FeatureToggleCallbackController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "feature.toggles", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(FeatureToggleProperties.class)
public class ToggleClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "toggleServerRestClient")
    RestClient toggleServerRestClient(FeatureToggleProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.serverBaseUrl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ToggleServerClient toggleServerClient(@Qualifier("toggleServerRestClient") RestClient toggleServerRestClient,
                                          FeatureToggleProperties properties) {
        return new HttpToggleServerClient(toggleServerRestClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    ToggleCache toggleCache() {
        return new ToggleCache();
    }

    @Bean
    @ConditionalOnMissingBean
    ToggleRuntimeService toggleRuntimeService(FeatureToggleProperties properties,
                                              ToggleCache toggleCache,
                                              ToggleServerClient toggleServerClient) {
        return new ToggleRuntimeService(properties, toggleCache, toggleServerClient);
    }

    @Bean
    @ConditionalOnMissingBean
    FeatureToggleClient featureToggleClient(ToggleRuntimeService toggleRuntimeService) {
        return toggleRuntimeService;
    }

    @Bean
    @ConditionalOnMissingBean(name = "featureToggleTaskScheduler")
    ThreadPoolTaskScheduler featureToggleTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("feature-toggle-client-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    ClientLifecycleService clientLifecycleService(FeatureToggleProperties properties,
                                                  ToggleServerClient toggleServerClient,
                                                  ToggleCache toggleCache,
                                                  @Qualifier("featureToggleTaskScheduler")
                                                  TaskScheduler featureToggleTaskScheduler) {
        return new ClientLifecycleService(properties, toggleServerClient, toggleCache, featureToggleTaskScheduler);
    }

    @Bean
    @ConditionalOnMissingBean
    FeatureToggleCallbackController featureToggleCallbackController(ToggleCache toggleCache) {
        return new FeatureToggleCallbackController(toggleCache);
    }
}
