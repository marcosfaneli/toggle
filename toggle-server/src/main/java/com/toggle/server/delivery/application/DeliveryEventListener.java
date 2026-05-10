package com.toggle.server.delivery.application;

import com.toggle.server.delivery.domain.ToggleUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DeliveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventListener.class);

    private final DeliverToggleUpdateUseCase useCase;

    public DeliveryEventListener(DeliverToggleUpdateUseCase useCase) {
        this.useCase = useCase;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onToggleUpdated(ToggleUpdatedEvent event) {
        log.info("event=toggle_delivery_event_received toggleName={} targetVersion={}", 
                event.toggleName(), event.targetVersion());
        useCase.execute(event);
    }
}
