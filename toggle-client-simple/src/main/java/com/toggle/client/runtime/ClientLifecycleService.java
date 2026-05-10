package com.toggle.client.runtime;

import com.toggle.client.config.FeatureToggleProperties;
import com.toggle.client.infra.ToggleServerClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ClientLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ClientLifecycleService.class);

    private final FeatureToggleProperties properties;
    private final ToggleServerClient serverClient;
    private final ToggleCache toggleCache;
    private final AtomicBoolean bootstrapInProgress = new AtomicBoolean(false);

    public ClientLifecycleService(FeatureToggleProperties properties,
                                  ToggleServerClient serverClient,
                                  ToggleCache toggleCache) {
        this.properties = properties;
        this.serverClient = serverClient;
        this.toggleCache = toggleCache;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        tryRegisterAndBootstrap();
    }

    @Scheduled(fixedDelayString = "${feature.toggles.heartbeat-interval-ms:30000}")
    public void sendHeartbeat() {
        if (!serverClient.isRegistered()) {
            tryRegisterAndBootstrap();
            return;
        }

        log.info("Sending heartbeat for instance {}", properties.instanceId());

        try {
            serverClient.heartbeat();
        } catch (RuntimeException ex) {
            log.warn("Heartbeat failed for instance {}: {}", properties.instanceId(), ex.getMessage());
        }
    }

    @PreDestroy
    public void onShutdown() {
        if (serverClient.isRegistered()) {
            serverClient.deregister();
        }
    }

    private void tryRegisterAndBootstrap() {
        if (!bootstrapInProgress.compareAndSet(false, true)) {
            return;
        }

        try {
            var snapshot = serverClient.registerAndGetSnapshot();
            if (!serverClient.isRegistered()) {
                log.warn("Client is not registered yet. Will retry automatically. instanceId={}",
                        properties.instanceId());
                return;
            }

            snapshot.forEach(toggleCache::putIfNewer);
            for (String toggleName : properties.consumed().keySet()) {
                serverClient.fetchToggleByName(toggleName).ifPresent(toggleCache::putIfNewer);
            }

            log.info("Client registered and bootstrap completed. consumedToggles={} instanceId={}",
                    properties.consumed().size(), properties.instanceId());
        } catch (RuntimeException ex) {
            log.warn("Client registration/bootstrap failed for instance {}: {}", properties.instanceId(), ex.getMessage());
        } finally {
            bootstrapInProgress.set(false);
        }
    }
}
