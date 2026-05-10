package com.toggle.client.infra;

import com.toggle.client.config.FeatureToggleProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServerClientConfig {

    @Bean
    RestClient toggleServerRestClient(FeatureToggleProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.serverBaseUrl())
                .build();
    }
}
