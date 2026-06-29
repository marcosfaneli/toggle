package com.toggle.server.client.persistence;

import com.toggle.server.client.domain.ClientInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-28T12:00:00Z");

    @Mock
    private ClientInstanceRepository instanceRepository;

    @Mock
    private ClientSubscriptionRepository subscriptionRepository;

    private ClientPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ClientPersistenceAdapter(
                instanceRepository,
                subscriptionRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldFindClientsFilteredByStatus() {
        var instance = aClientInstance("ACTIVE");
        var subscription = aSubscription();

        when(instanceRepository.findAllByStatusOrderByServiceNameAscInstanceIdAsc("ACTIVE"))
                .thenReturn(List.of(instance));
        when(subscriptionRepository.findAllByClientInstanceIdInOrderByToggleNameAsc(List.of(1L)))
                .thenReturn(List.of(subscription));

        var clients = adapter.findClients(null, ClientInstanceStatus.ACTIVE);

        assertThat(clients).hasSize(1);
        assertThat(clients.getFirst().status()).isEqualTo("ACTIVE");
        assertThat(clients.getFirst().subscriptions()).hasSize(1);
        verify(instanceRepository).findAllByStatusOrderByServiceNameAscInstanceIdAsc("ACTIVE");
    }

    @Test
    void shouldFindClientsFilteredByServiceNameAndStatus() {
        var instance = aClientInstance("INACTIVE");

        when(instanceRepository.findAllByServiceNameAndStatusOrderByInstanceIdAsc(
                "checkout-service",
                "INACTIVE"))
                .thenReturn(List.of(instance));
        when(subscriptionRepository.findAllByClientInstanceIdInOrderByToggleNameAsc(List.of(1L)))
                .thenReturn(List.of());

        var clients = adapter.findClients("checkout-service", ClientInstanceStatus.INACTIVE);

        assertThat(clients).hasSize(1);
        assertThat(clients.getFirst().serviceName()).isEqualTo("checkout-service");
        assertThat(clients.getFirst().status()).isEqualTo("INACTIVE");
        verify(instanceRepository).findAllByServiceNameAndStatusOrderByInstanceIdAsc(
                "checkout-service",
                "INACTIVE");
    }

    private static ClientInstanceEntity aClientInstance(String status) {
        var entity = new ClientInstanceEntity();
        entity.setId(1L);
        entity.setPublicId("01INSTANCE00000000000000000");
        entity.setServiceName("checkout-service");
        entity.setInstanceId("checkout-7d8d4c7f6f-abcde");
        entity.setPodName("checkout-7d8d4c7f6f-abcde");
        entity.setNamespace("payments");
        entity.setCallbackUrl("http://10.42.1.25:8080/internal/feature-toggles");
        entity.setStatus(status);
        entity.setRegisteredAt(NOW);
        return entity;
    }

    private static ClientSubscriptionEntity aSubscription() {
        var entity = new ClientSubscriptionEntity();
        entity.setId(10L);
        entity.setPublicId("01SUBSCRIPTION000000000000");
        entity.setClientInstanceId(1L);
        entity.setToggleName("new-checkout");
        entity.setConsumeMode("LOCAL_CACHE");
        entity.setCreatedAt(NOW);
        return entity;
    }
}
