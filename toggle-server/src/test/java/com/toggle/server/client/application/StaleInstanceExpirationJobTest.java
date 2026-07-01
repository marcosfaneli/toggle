package com.toggle.server.client.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleInstanceExpirationJobTest {

    private static final Instant NOW = Instant.parse("2026-06-20T18:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ClientPersistenceAdapter clientPersistenceAdapter;

    @Test
    void expireStaleInstances_calculatesThresholdAndDelegates() {
        var job = new StaleInstanceExpirationJob(clientPersistenceAdapter, CLOCK, 5L);

        when(clientPersistenceAdapter.expireStaleInstances(any())).thenReturn(2);

        job.expireStaleInstances();

        var thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(clientPersistenceAdapter).expireStaleInstances(thresholdCaptor.capture());

        Instant expectedThreshold = NOW.minus(5, ChronoUnit.MINUTES);
        assertThat(thresholdCaptor.getValue()).isEqualTo(expectedThreshold);
    }

    @Test
    void expireStaleInstances_whenNoneExpired_doesNotThrow() {
        var job = new StaleInstanceExpirationJob(clientPersistenceAdapter, CLOCK, 10L);

        when(clientPersistenceAdapter.expireStaleInstances(any())).thenReturn(0);

        job.expireStaleInstances();

        var thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(clientPersistenceAdapter).expireStaleInstances(thresholdCaptor.capture());

        Instant expectedThreshold = NOW.minus(10, ChronoUnit.MINUTES);
        assertThat(thresholdCaptor.getValue()).isEqualTo(expectedThreshold);
    }
}
