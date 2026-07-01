package com.toggle.server.delivery.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class DeliveryPersistenceAdapter {

    private static final Logger log = LoggerFactory.getLogger(DeliveryPersistenceAdapter.class);

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
        var found = repository.findById(syncStateId);
        if (found.isEmpty()) {
            log.warn("event=sync_state_not_found syncStateId={} action=markSynced", syncStateId);
            return;
        }
        var entity = found.get();
        entity.setSyncStatus("SYNCED");
        entity.setLastDeliveredVersion(deliveredVersion);
        entity.setLastResponseAt(Instant.now(clock));
        entity.setLastError(null);
        repository.save(entity);
    }

    @Transactional
    public void markOutOfSync(Long syncStateId, String error) {
        var found = repository.findById(syncStateId);
        if (found.isEmpty()) {
            log.warn("event=sync_state_not_found syncStateId={} action=markOutOfSync", syncStateId);
            return;
        }
        var entity = found.get();
        entity.setSyncStatus("OUT_OF_SYNC");
        entity.setLastResponseAt(Instant.now(clock));
        String trimmed = error != null && error.length() > 512 ? error.substring(0, 512) : error;
        entity.setLastError(trimmed);
        repository.save(entity);
    }
}
