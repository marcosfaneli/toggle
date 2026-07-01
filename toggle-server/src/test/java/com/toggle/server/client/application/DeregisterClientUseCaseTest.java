package com.toggle.server.client.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeregisterClientUseCaseTest {

    @Mock
    private ClientPersistenceAdapter clientPersistenceAdapter;

    @InjectMocks
    private DeregisterClientUseCase useCase;

    @Test
    void execute_delegatesDeactivation() {
        useCase.execute("checkout-service", "instance-123");

        verify(clientPersistenceAdapter).deactivate("checkout-service", "instance-123");
    }
}
