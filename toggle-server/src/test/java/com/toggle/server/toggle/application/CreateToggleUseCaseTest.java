package com.toggle.server.toggle.application;

import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateToggleUseCaseTest {

    @Mock
    private TogglePersistenceAdapter persistenceAdapter;

    @InjectMocks
    private CreateToggleUseCase useCase;

    @Test
    void execute_delegatesToPersistenceAdapter() {
        var command = new CreateToggleCommand("my-feature", "order-service", true, null);
        var expected = new Toggle("pub-id", "my-feature", "order-service", true, 1L, Instant.now(), null);

        when(persistenceAdapter.save(command)).thenReturn(expected);

        Toggle result = useCase.execute(command);

        assertThat(result).isSameAs(expected);
        verify(persistenceAdapter).save(command);
    }

    @Test
    void execute_withValue_delegatesToPersistenceAdapter() {
        var valueCmd = new CreateToggleCommand.ToggleValueCommand("STRING", "variant-a");
        var command = new CreateToggleCommand("feature-x", "payment-service", false, valueCmd);
        var expected = new Toggle("pub-id-2", "feature-x", "payment-service", false, 1L, Instant.now(), null);

        when(persistenceAdapter.save(command)).thenReturn(expected);

        Toggle result = useCase.execute(command);

        assertThat(result).isSameAs(expected);
        verify(persistenceAdapter).save(command);
    }
}
