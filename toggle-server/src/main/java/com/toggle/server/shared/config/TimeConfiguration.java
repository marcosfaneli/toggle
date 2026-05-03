package com.toggle.server.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;
import java.time.ZoneOffset;

@EnableAsync
@Configuration
public class TimeConfiguration {

    @Bean
    public Clock appClock() {
        return Clock.system(ZoneOffset.UTC);
    }
}