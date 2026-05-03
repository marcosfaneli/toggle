package com.toggle.server.client.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.springframework.stereotype.Service;

@Service
public class DeregisterClientUseCase {

    private final ClientPersistenceAdapter clientPersistenceAdapter;

    public DeregisterClientUseCase(ClientPersistenceAdapter clientPersistenceAdapter) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
    }

    public void execute(String serviceName, String instanceId) {
        clientPersistenceAdapter.deactivate(serviceName, instanceId);
    }
}
