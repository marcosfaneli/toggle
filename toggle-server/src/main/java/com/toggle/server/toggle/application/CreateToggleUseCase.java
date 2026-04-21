package com.toggle.server.toggle.application;

import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleAlreadyExistsException;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.springframework.stereotype.Service;

@Service
public class CreateToggleUseCase {

    private final TogglePersistenceAdapter persistenceAdapter;

    public CreateToggleUseCase(TogglePersistenceAdapter persistenceAdapter) {
        this.persistenceAdapter = persistenceAdapter;
    }

    public Toggle execute(CreateToggleCommand command) {
        if (persistenceAdapter.existsByNameAndOwner(command.name(), command.ownerServiceName())) {
            throw new ToggleAlreadyExistsException(command.name(), command.ownerServiceName());
        }
        return persistenceAdapter.save(command);
    }
}
