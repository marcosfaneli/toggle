package com.toggle.server.client.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StaleInstanceExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(StaleInstanceExpirationJob.class);

    private final ClientPersistenceAdapter clientPersistenceAdapter;
    private final long expirationTimeoutMinutes;

    public StaleInstanceExpirationJob(
            ClientPersistenceAdapter clientPersistenceAdapter,
            @Value("${toggle.heartbeat.expiration-timeout-minutes:5}") long expirationTimeoutMinutes) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
        this.expirationTimeoutMinutes = expirationTimeoutMinutes;
    }

    @Scheduled(fixedDelayString = "${toggle.heartbeat.expiration-check-interval-ms:60000}")
    public void expireStaleInstances() {
        var threshold = LocalDateTime.now().minusMinutes(expirationTimeoutMinutes);
        int expired = clientPersistenceAdapter.expireStaleInstances(threshold);
        if (expired > 0) {
            log.info("Expired {} stale client instance(s) with last heartbeat before {}", expired, threshold);
        }
    }
}
