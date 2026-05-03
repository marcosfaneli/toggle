package com.toggle.server.delivery.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import com.toggle.server.delivery.domain.ToggleUpdatedEvent;
import com.toggle.server.delivery.infrastructure.ToggleCallbackClient;
import com.toggle.server.delivery.infrastructure.ToggleCallbackPayload;
import com.toggle.server.delivery.persistence.DeliveryPersistenceAdapter;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleValue;
import com.toggle.server.toggle.domain.ValueType;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverToggleUpdateUseCaseTest {

    @Mock
    private TogglePersistenceAdapter toggleAdapter;

    @Mock
    private ClientPersistenceAdapter clientAdapter;

    @Mock
    private DeliveryPersistenceAdapter deliveryAdapter;

    @Mock
    private ToggleCallbackClient callbackClient;

    @InjectMocks
    private DeliverToggleUpdateUseCase useCase;

    private static final Toggle TOGGLE = new Toggle(
            "public-id-1",
            "my-feature",
            "my-service",
            true,
            3L,
            Instant.parse("2026-05-03T10:00:00Z"),
            null);

    private static final Toggle TOGGLE_WITH_VALUE = new Toggle(
            "public-id-2",
            "my-feature",
            "my-service",
            true,
            3L,
            Instant.parse("2026-05-03T10:00:00Z"),
            new ToggleValue(ValueType.STRING, "blue"));

    private static final ToggleUpdatedEvent EVENT = new ToggleUpdatedEvent("my-feature", "my-service", 3L);

    @Test
    void deliver_whenToggleNotFound_doesNothing() {
        when(toggleAdapter.findToggleInternalId("my-feature", "my-service")).thenReturn(Optional.empty());

        useCase.execute(EVENT);

        verify(clientAdapter, never()).findActiveSubscribersForToggle(anyString());
        verify(callbackClient, never()).deliver(anyString(), anyString(), any());
    }

    @Test
    void deliver_whenNoActiveSubscribers_doesNotCallHttp() {
        when(toggleAdapter.findToggleInternalId("my-feature", "my-service"))
                .thenReturn(Optional.of(new TogglePersistenceAdapter.ToggleInternalId(10L, TOGGLE)));
        when(clientAdapter.findActiveSubscribersForToggle("my-feature")).thenReturn(List.of());

        useCase.execute(EVENT);

        verify(callbackClient, never()).deliver(anyString(), anyString(), any());
        verify(deliveryAdapter, never()).upsertPendingResponse(anyLong(), anyLong(), anyLong());
    }

    @Test
    void deliver_whenCallbackSucceeds_marksSynced() {
        var subscriber = new ClientPersistenceAdapter.SubscriberView(42L, "http://client-svc:8080/toggles");
        when(toggleAdapter.findToggleInternalId("my-feature", "my-service"))
                .thenReturn(Optional.of(new TogglePersistenceAdapter.ToggleInternalId(10L, TOGGLE)));
        when(clientAdapter.findActiveSubscribersForToggle("my-feature")).thenReturn(List.of(subscriber));
        when(deliveryAdapter.upsertPendingResponse(10L, 42L, 3L)).thenReturn(99L);
        when(callbackClient.deliver(eq("http://client-svc:8080/toggles"), eq("my-feature"), any())).thenReturn(true);

        useCase.execute(EVENT);

        verify(deliveryAdapter).upsertPendingResponse(10L, 42L, 3L);
        verify(deliveryAdapter).markSynced(99L, 3L);
        verify(deliveryAdapter, never()).markOutOfSync(anyLong(), anyString());
    }

    @Test
    void deliver_whenCallbackFails_marksOutOfSync() {
        var subscriber = new ClientPersistenceAdapter.SubscriberView(42L, "http://client-svc:8080/toggles");
        when(toggleAdapter.findToggleInternalId("my-feature", "my-service"))
                .thenReturn(Optional.of(new TogglePersistenceAdapter.ToggleInternalId(10L, TOGGLE)));
        when(clientAdapter.findActiveSubscribersForToggle("my-feature")).thenReturn(List.of(subscriber));
        when(deliveryAdapter.upsertPendingResponse(10L, 42L, 3L)).thenReturn(99L);
        when(callbackClient.deliver(anyString(), anyString(), any())).thenReturn(false);

        useCase.execute(EVENT);

        verify(deliveryAdapter).markOutOfSync(eq(99L), anyString());
        verify(deliveryAdapter, never()).markSynced(anyLong(), anyLong());
    }

    @Test
    void deliver_payloadIncludesValueWhenPresent() {
        var subscriber = new ClientPersistenceAdapter.SubscriberView(42L, "http://client-svc:8080/toggles");
        when(toggleAdapter.findToggleInternalId("my-feature", "my-service"))
                .thenReturn(Optional.of(new TogglePersistenceAdapter.ToggleInternalId(10L, TOGGLE_WITH_VALUE)));
        when(clientAdapter.findActiveSubscribersForToggle("my-feature")).thenReturn(List.of(subscriber));
        when(deliveryAdapter.upsertPendingResponse(anyLong(), anyLong(), anyLong())).thenReturn(99L);
        when(callbackClient.deliver(anyString(), anyString(), any())).thenReturn(true);

        useCase.execute(EVENT);

        var payloadCaptor = ArgumentCaptor.forClass(ToggleCallbackPayload.class);
        verify(callbackClient).deliver(anyString(), anyString(), payloadCaptor.capture());

        var payload = payloadCaptor.getValue();
        assertThat(payload.value()).isNotNull();
        assertThat(payload.value().type()).isEqualTo("STRING");
        assertThat(payload.value().raw()).isEqualTo("blue");
    }

    @Test
    void deliver_payloadHasNullValueWhenToggleHasNoValue() {
        var subscriber = new ClientPersistenceAdapter.SubscriberView(42L, "http://client-svc:8080/toggles");
        when(toggleAdapter.findToggleInternalId("my-feature", "my-service"))
                .thenReturn(Optional.of(new TogglePersistenceAdapter.ToggleInternalId(10L, TOGGLE)));
        when(clientAdapter.findActiveSubscribersForToggle("my-feature")).thenReturn(List.of(subscriber));
        when(deliveryAdapter.upsertPendingResponse(anyLong(), anyLong(), anyLong())).thenReturn(99L);
        when(callbackClient.deliver(anyString(), anyString(), any())).thenReturn(true);

        useCase.execute(EVENT);

        var payloadCaptor = ArgumentCaptor.forClass(ToggleCallbackPayload.class);
        verify(callbackClient).deliver(anyString(), anyString(), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue().value()).isNull();
    }
}
