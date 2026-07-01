package com.toggle.server.toggle.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.application.UpdateToggleCommand;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleAlreadyExistsException;
import com.toggle.server.toggle.domain.ToggleNotFoundException;
import com.toggle.server.toggle.domain.ToggleValue;
import com.toggle.server.toggle.domain.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class TogglePersistenceAdapter {

    private static final Logger log = LoggerFactory.getLogger(TogglePersistenceAdapter.class);

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
    public Slice<Toggle> findAll(String maintainer, Boolean enabled, Pageable pageable) {
        var hasMaintainerFilter = maintainer != null && !maintainer.isBlank();

        if (hasMaintainerFilter && enabled != null) {
            return repository.findByMaintainerAndEnabled(maintainer, enabled, pageable)
                    .map(this::toDomain);
        }

        if (hasMaintainerFilter) {
            return repository.findByMaintainer(maintainer, pageable)
                    .map(this::toDomain);
        }

        if (enabled != null) {
            return repository.findByEnabled(enabled, pageable)
                    .map(this::toDomain);
        }

        return repository.findAll(pageable)
                .map(this::toDomain);
    }

    @Transactional
    public Toggle save(CreateToggleCommand command) {
        var entity = new ToggleEntity();
        entity.setPublicId(UlidCreator.getMonotonicUlid().toString());
        entity.setName(command.name());
        entity.setMaintainer(command.maintainer());
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
                .orElseThrow(() -> {
                    log.info("event=toggle_update_rejected name={} reason=not_found", command.name());
                    return new ToggleNotFoundException(command.name());
                });

        if (command.enabled() != null) {
            entity.setEnabled(command.enabled());
        }

        if (command.maintainer() != null) {
            entity.setMaintainer(command.maintainer());
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

        var saved = toDomain(repository.save(entity));
        log.info(
                "event=toggle_updated name={} version={} maintainer={} enabled={} hasValue={}",
                saved.name(),
                saved.version(),
                saved.maintainer(),
                saved.enabled(),
                saved.value() != null);
        return saved;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ToggleInternalId> findToggleInternalId(String name) {
        return repository.findByName(name)
                .map(e -> new ToggleInternalId(e.getId(), toDomain(e)));
    }

    @Transactional(readOnly = true)
    public Optional<Toggle> findByName(String name) {
        return repository.findByName(name).map(this::toDomain);
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
                entity.getMaintainer(),
                entity.isEnabled(),
                entity.getVersion(),
                entity.getUpdatedAt(),
                value);
    }
}
