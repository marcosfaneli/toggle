package com.toggle.server.toggle.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TogglePersistenceAdapter {

    private final ToggleRepository repository;

    public TogglePersistenceAdapter(ToggleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Toggle save(CreateToggleCommand command) {
        var entity = new ToggleEntity();
        entity.setPublicId(UlidCreator.getMonotonicUlid().toString());
        entity.setName(command.name());
        entity.setOwnerServiceName(command.ownerServiceName());
        entity.setEnabled(command.enabled());
        entity.setVersion(1L);
        entity.setUpdatedAt(LocalDateTime.now());

        try {
            return toDomain(repository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ToggleAlreadyExistsException(command.name(), command.ownerServiceName());
        }
    }

    private Toggle toDomain(ToggleEntity entity) {
        return new Toggle(
                entity.getPublicId(),
                entity.getName(),
                entity.getOwnerServiceName(),
                entity.isEnabled(),
                entity.getVersion(),
                entity.getUpdatedAt());
    }
}
