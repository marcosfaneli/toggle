package com.toggle.server.toggle.application;

import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.springframework.stereotype.Service;

@Service
public class UpdateToggleUseCase {

    private final TogglePersistenceAdapter persistenceAdapter;

    public UpdateToggleUseCase(TogglePersistenceAdapter persistenceAdapter) {
        this.persistenceAdapter = persistenceAdapter;
    }

    public Toggle execute(UpdateToggleCommand command) {
        return persistenceAdapter.update(command);
    }
}
