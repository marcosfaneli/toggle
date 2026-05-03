package com.toggle.server.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class TimeConfiguration {

    @Bean
    public Clock appClock() {
        return Clock.system(ZoneOffset.UTC);
    }
}