package com.toggle.server.toggle.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.domain.Toggle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TogglePersistenceAdapter {

    private final ToggleRepository repository;

    public TogglePersistenceAdapter(ToggleRepository repository) {
        this.repository = repository;
    }

    public boolean existsByNameAndOwner(String name, String ownerServiceName) {
        return repository.existsByNameAndOwnerServiceName(name, ownerServiceName);
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

        return toDomain(repository.save(entity));
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
