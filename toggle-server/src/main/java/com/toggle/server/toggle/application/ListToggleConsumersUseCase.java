package com.toggle.server.toggle.application;

import com.toggle.server.client.application.ToggleConsumerView;
import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class ListToggleConsumersUseCase {

    private final ClientPersistenceAdapter clientPersistenceAdapter;

    public ListToggleConsumersUseCase(ClientPersistenceAdapter clientPersistenceAdapter) {
        this.clientPersistenceAdapter = clientPersistenceAdapter;
    }

    public Slice<ToggleConsumerView> execute(String toggleName, Pageable pageable) {
        return clientPersistenceAdapter.findConsumersForToggle(toggleName, pageable);
    }
}
