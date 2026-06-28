package com.toggle.server.client.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListClientsUseCase {

    private final ClientPersistenceAdapter clientPersistenceAdapter;

    public ListClientsUseCase(ClientPersistenceAdapter clientPersistenceAdapter) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
    }

    public List<ClientView> execute() {
        return execute(null);
    }

    public List<ClientView> execute(String serviceName) {
        return clientPersistenceAdapter.findClients(serviceName);
    }
}
