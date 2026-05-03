package com.toggle.server.delivery.application;

import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import com.toggle.server.delivery.domain.ToggleUpdatedEvent;
import com.toggle.server.delivery.infrastructure.ToggleCallbackClient;
import com.toggle.server.delivery.infrastructure.ToggleCallbackPayload;
import com.toggle.server.delivery.persistence.DeliveryPersistenceAdapter;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeliverToggleUpdateUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeliverToggleUpdateUseCase.class);

    private final TogglePersistenceAdapter toggleAdapter;
    private final ClientPersistenceAdapter clientAdapter;
    private final DeliveryPersistenceAdapter deliveryAdapter;
    private final ToggleCallbackClient callbackClient;

    public DeliverToggleUpdateUseCase(TogglePersistenceAdapter toggleAdapter,
                                      ClientPersistenceAdapter clientAdapter,
                                      DeliveryPersistenceAdapter deliveryAdapter,
                                      ToggleCallbackClient callbackClient) {
        this.toggleAdapter = toggleAdapter;
        this.clientAdapter = clientAdapter;
        this.deliveryAdapter = deliveryAdapter;
        this.callbackClient = callbackClient;
    }

    public void execute(ToggleUpdatedEvent event) {
        var found = toggleAdapter.findToggleInternalId(event.toggleName(), event.ownerServiceName());
        if (found.isEmpty()) {
            log.warn("Toggle not found during delivery: name={}, owner={}", event.toggleName(), event.ownerServiceName());
            return;
        }

        var toggleWithId = found.get();
        Long toggleId = toggleWithId.id();
        Toggle toggle = toggleWithId.toggle();

        var subscribers = clientAdapter.findActiveSubscribersForToggle(event.toggleName());
        if (subscribers.isEmpty()) {
            return;
        }

        ToggleCallbackPayload payload = toPayload(toggle);

        for (var subscriber : subscribers) {
            Long syncStateId = deliveryAdapter.upsertPendingResponse(
                    toggleId, subscriber.clientInstanceId(), event.targetVersion());

            boolean delivered = callbackClient.deliver(
                    subscriber.callbackUrl(), event.toggleName(), payload);

            if (delivered) {
                deliveryAdapter.markSynced(syncStateId, event.targetVersion());
            } else {
                deliveryAdapter.markOutOfSync(syncStateId,
                        "HTTP PUT failed for callback: " + subscriber.callbackUrl());
            }
        }
    }

    private ToggleCallbackPayload toPayload(Toggle toggle) {
        ToggleCallbackPayload.ValuePayload valuePayload = null;
        if (toggle.value() != null) {
            valuePayload = new ToggleCallbackPayload.ValuePayload(
                    toggle.value().type().name(),
                    toggle.value().raw());
        }
        return new ToggleCallbackPayload(
                toggle.name(),
                toggle.ownerServiceName(),
                toggle.enabled(),
                toggle.version(),
                valuePayload);
    }
}
