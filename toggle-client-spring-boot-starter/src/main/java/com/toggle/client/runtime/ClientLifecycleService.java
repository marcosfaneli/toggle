package com.toggle.client.runtime;

import com.toggle.client.config.FeatureToggleProperties;
import com.toggle.client.infra.ToggleServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientLifecycleService implements ApplicationListener<ApplicationReadyEvent>, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ClientLifecycleService.class);

    private final FeatureToggleProperties properties;
    private final ToggleServerClient serverClient;
    private final ToggleCache toggleCache;
    private final TaskScheduler taskScheduler;
    private final AtomicBoolean bootstrapInProgress = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> heartbeatTask;

    public ClientLifecycleService(FeatureToggleProperties properties,
                                  ToggleServerClient serverClient,
                                  ToggleCache toggleCache,
                                  TaskScheduler taskScheduler) {
        this.properties = properties;
        this.serverClient = serverClient;
        this.toggleCache = toggleCache;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        tryRegisterAndBootstrap();
        heartbeatTask = taskScheduler.scheduleWithFixedDelay(
                this::sendHeartbeat,
                Duration.ofMillis(properties.heartbeatIntervalMs()));
    }

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

    @Override
    public void close() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
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
