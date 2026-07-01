package com.toggle.server.delivery.application;

import com.toggle.server.delivery.domain.ToggleUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryEventListenerTest {

    @Mock
    private DeliverToggleUpdateUseCase useCase;

    @InjectMocks
    private DeliveryEventListener listener;

    @Test
    void onToggleUpdated_delegatesToUseCase() {
        var event = new ToggleUpdatedEvent("my-feature", 3L);

        listener.onToggleUpdated(event);

        verify(useCase).execute(event);
    }

    @Test
    void onToggleUpdated_whenUseCaseThrows_doesNotPropagate() {
        var event = new ToggleUpdatedEvent("my-feature", 5L);
        doThrow(new RuntimeException("delivery failed")).when(useCase).execute(event);

        listener.onToggleUpdated(event);

        verify(useCase).execute(event);
    }
}
