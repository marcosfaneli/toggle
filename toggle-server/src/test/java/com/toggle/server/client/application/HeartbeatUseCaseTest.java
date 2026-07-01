package com.toggle.server.client.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HeartbeatUseCaseTest {

    @Mock
    private ClientPersistenceAdapter clientPersistenceAdapter;

    @InjectMocks
    private HeartbeatUseCase useCase;

    @Test
    void execute_delegatesHeartbeat() {
        var command = new HeartbeatCommand("checkout-service", "instance-456");

        useCase.execute(command);

        verify(clientPersistenceAdapter).heartbeat("checkout-service", "instance-456");
    }
}
