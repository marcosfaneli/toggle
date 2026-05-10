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
        try {
            var found = toggleAdapter.findToggleInternalId(event.toggleName());
            if (found.isEmpty()) {
                log.warn("event=toggle_not_found_during_delivery toggleName={} targetVersion={}",
                        event.toggleName(), event.targetVersion());
                return;
            }

            var toggleWithId = found.get();
            Long toggleId = toggleWithId.id();
            Toggle toggle = toggleWithId.toggle();

            var subscribers = clientAdapter.findActiveSubscribersForToggle(event.toggleName());
            log.info("event=toggle_delivery_started toggleName={} version={} subscribersCount={}",
                    event.toggleName(), event.targetVersion(), subscribers.size());
            if (subscribers.isEmpty()) {
                log.info("event=toggle_delivery_no_subscribers toggleName={} version={}",
                        event.toggleName(), event.targetVersion());
                return;
            }

            ToggleCallbackPayload payload = toPayload(toggle);

            for (var subscriber : subscribers) {
                try {
                    Long syncStateId = deliveryAdapter.upsertPendingResponse(
                            toggleId, subscriber.clientInstanceId(), event.targetVersion());

                    boolean delivered = callbackClient.deliver(
                            subscriber.callbackUrl(), event.toggleName(), payload);

                    if (delivered) {
                        deliveryAdapter.markSynced(syncStateId, event.targetVersion());
                        log.info("event=toggle_delivery_success toggleName={} version={} clientInstanceId={} callbackUrl={}",
                                event.toggleName(), event.targetVersion(), subscriber.clientInstanceId(),
                                subscriber.callbackUrl());
                    } else {
                        deliveryAdapter.markOutOfSync(syncStateId,
                                "HTTP PUT failed for callback: " + subscriber.callbackUrl());
                        log.warn("event=toggle_delivery_failed toggleName={} version={} clientInstanceId={} callbackUrl={} reason=http_error",
                                event.toggleName(), event.targetVersion(), subscriber.clientInstanceId(),
                                subscriber.callbackUrl());
                    }
                } catch (Exception ex) {
                    log.error("event=toggle_delivery_subscriber_error toggleName={} version={} clientInstanceId={} error={}",
                            event.toggleName(), event.targetVersion(), subscriber.clientInstanceId(),
                            ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            log.error("event=toggle_delivery_unexpected_error toggleName={} targetVersion={} error={}",
                    event.toggleName(), event.targetVersion(), ex.getMessage(), ex);
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
