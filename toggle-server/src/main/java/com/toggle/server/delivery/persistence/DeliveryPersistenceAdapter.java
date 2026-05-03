package com.toggle.server.delivery.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class DeliveryPersistenceAdapter {

    private final ToggleSyncStateRepository repository;
    private final Clock clock;

    public DeliveryPersistenceAdapter(ToggleSyncStateRepository repository, Clock appClock) {
        this.repository = repository;
        this.clock = appClock;
    }

    @Transactional
    public Long upsertPendingResponse(Long toggleId, Long clientInstanceId, long targetVersion) {
        var entity = repository.findByToggleIdAndClientInstanceId(toggleId, clientInstanceId)
                .orElseGet(ToggleSyncStateEntity::new);
        entity.setToggleId(toggleId);
        entity.setClientInstanceId(clientInstanceId);
        entity.setTargetVersion(targetVersion);
        entity.setSyncStatus("PENDING_RESPONSE");
        entity.setLastDeliveryAttemptAt(Instant.now(clock));
        if (entity.getId() != null) {
            entity.setRetryCount(entity.getRetryCount() + 1);
        }
        return repository.save(entity).getId();
    }

    @Transactional
    public void markSynced(Long syncStateId, long deliveredVersion) {
        repository.findById(syncStateId).ifPresent(entity -> {
            entity.setSyncStatus("SYNCED");
            entity.setLastDeliveredVersion(deliveredVersion);
            entity.setLastResponseAt(Instant.now(clock));
            entity.setLastError(null);
            repository.save(entity);
        });
    }

    @Transactional
    public void markOutOfSync(Long syncStateId, String error) {
        repository.findById(syncStateId).ifPresent(entity -> {
            entity.setSyncStatus("OUT_OF_SYNC");
            entity.setLastResponseAt(Instant.now(clock));
            String trimmed = error != null && error.length() > 512 ? error.substring(0, 512) : error;
            entity.setLastError(trimmed);
            repository.save(entity);
        });
    }
}
