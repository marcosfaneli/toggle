package com.toggle.server.toggle.application;

import com.toggle.server.delivery.domain.ToggleUpdatedEvent;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UpdateToggleUseCase {

    private final TogglePersistenceAdapter persistenceAdapter;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateToggleUseCase(TogglePersistenceAdapter persistenceAdapter,
                               ApplicationEventPublisher eventPublisher) {
        this.persistenceAdapter = persistenceAdapter;
        this.eventPublisher = eventPublisher;
    }

    public Toggle execute(UpdateToggleCommand command) {
        Toggle toggle = persistenceAdapter.update(command);
        eventPublisher.publishEvent(
                new ToggleUpdatedEvent(toggle.name(), toggle.ownerServiceName(), toggle.version()));
        return toggle;
    }
}
