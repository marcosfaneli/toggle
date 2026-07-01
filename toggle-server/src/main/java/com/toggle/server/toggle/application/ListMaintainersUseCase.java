package com.toggle.server.toggle.application;

import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMaintainersUseCase {

    private final TogglePersistenceAdapter persistenceAdapter;

    public ListMaintainersUseCase(TogglePersistenceAdapter persistenceAdapter) {
        this.persistenceAdapter = persistenceAdapter;
    }

    public List<String> execute() {
        return persistenceAdapter.findMaintainers();
    }
}
