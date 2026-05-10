package com.toggle.server.toggle.application;

import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleNotFoundException;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.springframework.stereotype.Service;

@Service
public class GetToggleByNameUseCase {

    private final TogglePersistenceAdapter persistenceAdapter;

    public GetToggleByNameUseCase(TogglePersistenceAdapter persistenceAdapter) {
        this.persistenceAdapter = persistenceAdapter;
    }

    public Toggle execute(String name) {
        return persistenceAdapter.findByName(name)
                .orElseThrow(() -> new ToggleNotFoundException(name));
    }
}
