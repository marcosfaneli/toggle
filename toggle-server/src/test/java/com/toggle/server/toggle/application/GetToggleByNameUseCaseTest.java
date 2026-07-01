package com.toggle.server.toggle.application;

import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.domain.ToggleNotFoundException;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetToggleByNameUseCaseTest {

    @Mock
    private TogglePersistenceAdapter persistenceAdapter;

    @InjectMocks
    private GetToggleByNameUseCase useCase;

    @Test
    void execute_whenToggleExists_returnsToggle() {
        var toggle = new Toggle("pub-id", "my-feature", "order-service", true, 1L, Instant.now(), null);
        when(persistenceAdapter.findByName("my-feature")).thenReturn(Optional.of(toggle));

        Toggle result = useCase.execute("my-feature");

        assertThat(result).isSameAs(toggle);
    }

    @Test
    void execute_whenToggleNotFound_throwsException() {
        when(persistenceAdapter.findByName("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("nonexistent"))
                .isInstanceOf(ToggleNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }
}
