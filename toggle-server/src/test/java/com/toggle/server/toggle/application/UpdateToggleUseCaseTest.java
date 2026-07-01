package com.toggle.server.toggle.application;

import com.toggle.server.delivery.domain.ToggleUpdatedEvent;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateToggleUseCaseTest {

    @Mock
    private TogglePersistenceAdapter persistenceAdapter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UpdateToggleUseCase useCase;

    @Test
    void execute_updatesAndPublishesEvent() {
        var command = new UpdateToggleCommand("my-feature", false, new UpdateToggleCommand.ValueUpdate.Keep());
        var updatedToggle = new Toggle("pub-id", "my-feature", "order-service", false, 2L, Instant.now(), null);

        when(persistenceAdapter.update(command)).thenReturn(updatedToggle);

        Toggle result = useCase.execute(command);

        assertThat(result).isSameAs(updatedToggle);
        verify(persistenceAdapter).update(command);

        var eventCaptor = ArgumentCaptor.forClass(ToggleUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ToggleUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.toggleName()).isEqualTo("my-feature");
        assertThat(event.targetVersion()).isEqualTo(2L);
    }

    @Test
    void execute_withValueRemoval_updatesAndPublishesEvent() {
        var command = new UpdateToggleCommand("another-toggle", true, new UpdateToggleCommand.ValueUpdate.Remove());
        var updatedToggle = new Toggle("pub-id-2", "another-toggle", "svc", true, 5L, Instant.now(), null);

        when(persistenceAdapter.update(command)).thenReturn(updatedToggle);

        Toggle result = useCase.execute(command);

        assertThat(result).isSameAs(updatedToggle);

        var eventCaptor = ArgumentCaptor.forClass(ToggleUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue().toggleName()).isEqualTo("another-toggle");
        assertThat(eventCaptor.getValue().targetVersion()).isEqualTo(5L);
    }
}
