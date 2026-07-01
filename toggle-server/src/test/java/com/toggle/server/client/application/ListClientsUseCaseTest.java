package com.toggle.server.client.application;

import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClientsUseCaseTest {

    @Mock
    private ClientPersistenceAdapter clientPersistenceAdapter;

    @InjectMocks
    private ListClientsUseCase useCase;

    private static final ClientView SAMPLE_VIEW = new ClientView(
            "pub-id-1", "checkout-service", "instance-1",
            "pod-1", "ns", "http://cb:8080/toggles",
            "ACTIVE", Instant.now(), List.of());

    @Test
    void execute_noArgs_delegatesWithNullParams() {
        when(clientPersistenceAdapter.findClients(null, null)).thenReturn(List.of(SAMPLE_VIEW));

        List<ClientView> result = useCase.execute();

        assertThat(result).containsExactly(SAMPLE_VIEW);
        verify(clientPersistenceAdapter).findClients(null, null);
    }

    @Test
    void execute_withServiceName_delegatesWithNullStatus() {
        when(clientPersistenceAdapter.findClients("checkout-service", null)).thenReturn(List.of(SAMPLE_VIEW));

        List<ClientView> result = useCase.execute("checkout-service");

        assertThat(result).containsExactly(SAMPLE_VIEW);
        verify(clientPersistenceAdapter).findClients("checkout-service", null);
    }

    @Test
    void execute_withServiceNameAndStatus_delegatesBothParams() {
        when(clientPersistenceAdapter.findClients("checkout-service", ClientInstanceStatus.ACTIVE))
                .thenReturn(List.of(SAMPLE_VIEW));

        List<ClientView> result = useCase.execute("checkout-service", ClientInstanceStatus.ACTIVE);

        assertThat(result).containsExactly(SAMPLE_VIEW);
        verify(clientPersistenceAdapter).findClients("checkout-service", ClientInstanceStatus.ACTIVE);
    }
}
