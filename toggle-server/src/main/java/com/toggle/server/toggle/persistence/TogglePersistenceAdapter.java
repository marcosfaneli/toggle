package com.toggle.server.toggle.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.application.UpdateToggleCommand;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleAlreadyExistsException;
import com.toggle.server.toggle.domain.ToggleNotFoundException;
import com.toggle.server.toggle.domain.ToggleValue;
import com.toggle.server.toggle.domain.ValueType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class TogglePersistenceAdapter {

    private final ToggleRepository repository;
    private final Clock clock;

    public TogglePersistenceAdapter(ToggleRepository repository, Clock appClock) {
        this.repository = repository;
        this.clock = appClock;
    }

    @Transactional(readOnly = true)
    public List<Toggle> findAllByNames(List<String> names) {
        return repository.findAllByNameIn(names).stream().map(this::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public Slice<Toggle> findAll(String ownerServiceName, Boolean enabled, Pageable pageable) {
        Slice<ToggleEntity> slice = enabled != null
                ? repository.findByOwnerServiceNameAndEnabled(ownerServiceName, enabled, pageable)
                : repository.findByOwnerServiceName(ownerServiceName, pageable);
        return slice.map(this::toDomain);
    }

    @Transactional
    public Toggle save(CreateToggleCommand command) {
        var entity = new ToggleEntity();
        entity.setPublicId(UlidCreator.getMonotonicUlid().toString());
        entity.setName(command.name());
        entity.setOwnerServiceName(command.ownerServiceName());
        entity.setEnabled(command.enabled());
        entity.setVersion(1L);
        entity.setUpdatedAt(Instant.now(clock));

        if (command.value() != null) {
            var valueEntity = new ToggleValueEntity();
            valueEntity.setToggle(entity);
            valueEntity.setValueType(command.value().type());
            valueEntity.setValueRaw(command.value().raw());
            valueEntity.setUpdatedAt(Instant.now(clock));
            entity.setValue(valueEntity);
        }

        try {
            return toDomain(repository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ToggleAlreadyExistsException(command.name());
        }
    }

    @Transactional
    public Toggle update(UpdateToggleCommand command) {
        var entity = repository.findByName(command.name())
                .orElseThrow(() -> new ToggleNotFoundException(command.name()));

        if (command.enabled() != null) {
            entity.setEnabled(command.enabled());
        }

        switch (command.valueUpdate()) {
            case UpdateToggleCommand.ValueUpdate.Keep ignored -> {}
            case UpdateToggleCommand.ValueUpdate.Remove ignored -> entity.setValue(null);
            case UpdateToggleCommand.ValueUpdate.Set set -> {
                if (entity.getValue() != null) {
                    entity.getValue().setValueType(set.type());
                    entity.getValue().setValueRaw(set.raw());
                    entity.getValue().setUpdatedAt(Instant.now(clock));
                } else {
                    var valueEntity = new ToggleValueEntity();
                    valueEntity.setToggle(entity);
                    valueEntity.setValueType(set.type());
                    valueEntity.setValueRaw(set.raw());
                    valueEntity.setUpdatedAt(Instant.now(clock));
                    entity.setValue(valueEntity);
                }
            }
        }

        entity.setVersion(entity.getVersion() + 1);
        entity.setUpdatedAt(Instant.now(clock));

        return toDomain(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ToggleInternalId> findToggleInternalId(String name, String ownerServiceName) {
        return repository.findByName(name)
                .map(e -> new ToggleInternalId(e.getId(), toDomain(e)));
    }

    public record ToggleInternalId(Long id, Toggle toggle) {}

    private Toggle toDomain(ToggleEntity entity) {
        ToggleValue value = null;
        if (entity.getValue() != null) {
            value = new ToggleValue(
                    ValueType.valueOf(entity.getValue().getValueType()),
                    entity.getValue().getValueRaw());
        }
        return new Toggle(
                entity.getPublicId(),
                entity.getName(),
                entity.getOwnerServiceName(),
                entity.isEnabled(),
                entity.getVersion(),
                entity.getUpdatedAt(),
                value);
    }
}
