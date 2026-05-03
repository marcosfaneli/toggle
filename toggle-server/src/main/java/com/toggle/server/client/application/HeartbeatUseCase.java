package com.toggle.server.client.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatUseCase {

    private final ClientPersistenceAdapter clientPersistenceAdapter;

    public HeartbeatUseCase(ClientPersistenceAdapter clientPersistenceAdapter) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
    }

    public void execute(HeartbeatCommand command) {
        clientPersistenceAdapter.heartbeat(command.serviceName(), command.instanceId());
    }
}
