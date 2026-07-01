package com.toggle.server.delivery.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-20T18:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ToggleSyncStateRepository repository;

    private DeliveryPersistenceAdapter adapter;

    private DeliveryPersistenceAdapter createAdapter() {
        return new DeliveryPersistenceAdapter(repository, CLOCK);
    }

    @Test
    void upsertPendingResponse_whenNewEntity_createsWithZeroRetry() {
        adapter = createAdapter();
        when(repository.findByToggleIdAndClientInstanceId(10L, 42L)).thenReturn(Optional.empty());

        var savedEntity = new ToggleSyncStateEntity();
        savedEntity.setId(99L);
        when(repository.save(any(ToggleSyncStateEntity.class))).thenReturn(savedEntity);

        Long result = adapter.upsertPendingResponse(10L, 42L, 3L);

        assertThat(result).isEqualTo(99L);

        var captor = ArgumentCaptor.forClass(ToggleSyncStateEntity.class);
        verify(repository).save(captor.capture());

        var entity = captor.getValue();
        assertThat(entity.getToggleId()).isEqualTo(10L);
        assertThat(entity.getClientInstanceId()).isEqualTo(42L);
        assertThat(entity.getTargetVersion()).isEqualTo(3L);
        assertThat(entity.getSyncStatus()).isEqualTo("PENDING_RESPONSE");
        assertThat(entity.getLastDeliveryAttemptAt()).isEqualTo(NOW);
    }

    @Test
    void upsertPendingResponse_whenExistingEntity_incrementsRetryCount() {
        adapter = createAdapter();
        var existingEntity = new ToggleSyncStateEntity();
        existingEntity.setId(50L);
        existingEntity.setRetryCount(2);
        when(repository.findByToggleIdAndClientInstanceId(10L, 42L)).thenReturn(Optional.of(existingEntity));

        var savedEntity = new ToggleSyncStateEntity();
        savedEntity.setId(50L);
        when(repository.save(any(ToggleSyncStateEntity.class))).thenReturn(savedEntity);

        Long result = adapter.upsertPendingResponse(10L, 42L, 5L);

        assertThat(result).isEqualTo(50L);

        var captor = ArgumentCaptor.forClass(ToggleSyncStateEntity.class);
        verify(repository).save(captor.capture());

        var entity = captor.getValue();
        assertThat(entity.getRetryCount()).isEqualTo(3);
        assertThat(entity.getTargetVersion()).isEqualTo(5L);
        assertThat(entity.getSyncStatus()).isEqualTo("PENDING_RESPONSE");
    }

    @Test
    void markSynced_whenEntityExists_updatesSyncedFields() {
        adapter = createAdapter();
        var entity = new ToggleSyncStateEntity();
        entity.setId(99L);
        entity.setSyncStatus("PENDING_RESPONSE");
        entity.setLastError("previous error");
        when(repository.findById(99L)).thenReturn(Optional.of(entity));
        when(repository.save(any(ToggleSyncStateEntity.class))).thenReturn(entity);

        adapter.markSynced(99L, 3L);

        var captor = ArgumentCaptor.forClass(ToggleSyncStateEntity.class);
        verify(repository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getSyncStatus()).isEqualTo("SYNCED");
        assertThat(saved.getLastDeliveredVersion()).isEqualTo(3L);
        assertThat(saved.getLastResponseAt()).isEqualTo(NOW);
        assertThat(saved.getLastError()).isNull();
    }

    @Test
    void markSynced_whenEntityNotFound_doesNothing() {
        adapter = createAdapter();
        when(repository.findById(999L)).thenReturn(Optional.empty());

        adapter.markSynced(999L, 3L);

        verify(repository, never()).save(any());
    }

    @Test
    void markOutOfSync_whenEntityExists_setsStatusAndError() {
        adapter = createAdapter();
        var entity = new ToggleSyncStateEntity();
        entity.setId(99L);
        when(repository.findById(99L)).thenReturn(Optional.of(entity));
        when(repository.save(any(ToggleSyncStateEntity.class))).thenReturn(entity);

        adapter.markOutOfSync(99L, "http_error");

        var captor = ArgumentCaptor.forClass(ToggleSyncStateEntity.class);
        verify(repository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getSyncStatus()).isEqualTo("OUT_OF_SYNC");
        assertThat(saved.getLastResponseAt()).isEqualTo(NOW);
        assertThat(saved.getLastError()).isEqualTo("http_error");
    }

    @Test
    void markOutOfSync_whenEntityNotFound_doesNothing() {
        adapter = createAdapter();
        when(repository.findById(999L)).thenReturn(Optional.empty());

        adapter.markOutOfSync(999L, "some error");

        verify(repository, never()).save(any());
    }

    @Test
    void markOutOfSync_truncatesErrorAt512Characters() {
        adapter = createAdapter();
        var entity = new ToggleSyncStateEntity();
        entity.setId(99L);
        when(repository.findById(99L)).thenReturn(Optional.of(entity));
        when(repository.save(any(ToggleSyncStateEntity.class))).thenReturn(entity);

        String longError = "x".repeat(600);
        adapter.markOutOfSync(99L, longError);

        var captor = ArgumentCaptor.forClass(ToggleSyncStateEntity.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getLastError()).hasSize(512);
    }
}
