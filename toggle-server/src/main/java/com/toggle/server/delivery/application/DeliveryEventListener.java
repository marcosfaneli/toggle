package com.toggle.server.delivery.application;

import com.toggle.server.delivery.domain.ToggleUpdatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DeliveryEventListener {

    private final DeliverToggleUpdateUseCase useCase;

    public DeliveryEventListener(DeliverToggleUpdateUseCase useCase) {
        this.useCase = useCase;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onToggleUpdated(ToggleUpdatedEvent event) {
        useCase.execute(event);
    }
}
