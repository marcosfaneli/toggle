package com.toggle.server.toggle.application;

import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTogglesUseCaseTest {

    @Mock
    private TogglePersistenceAdapter persistenceAdapter;

    @InjectMocks
    private ListTogglesUseCase useCase;

    @Test
    void execute_delegatesWithOwnerAndEnabled() {
        var query = new ListTogglesQuery("order-service", true);
        Pageable pageable = PageRequest.of(0, 10);
        var toggle = new Toggle("pub-id", "feature-a", "order-service", true, 1L, Instant.now(), null);
        Slice<Toggle> expected = new SliceImpl<>(List.of(toggle));

        when(persistenceAdapter.findAll("order-service", true, pageable)).thenReturn(expected);

        Slice<Toggle> result = useCase.execute(query, pageable);

        assertThat(result).isSameAs(expected);
        verify(persistenceAdapter).findAll("order-service", true, pageable);
    }

    @Test
    void execute_delegatesWithNullFilters() {
        var query = new ListTogglesQuery(null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Slice<Toggle> expected = new SliceImpl<>(List.of());

        when(persistenceAdapter.findAll(null, null, pageable)).thenReturn(expected);

        Slice<Toggle> result = useCase.execute(query, pageable);

        assertThat(result).isSameAs(expected);
        verify(persistenceAdapter).findAll(null, null, pageable);
    }
}
