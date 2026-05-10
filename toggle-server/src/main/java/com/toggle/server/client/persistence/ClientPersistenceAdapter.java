package com.toggle.server.client.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceInactiveException;
import com.toggle.server.client.domain.ClientInstanceNotFoundException;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.ClientSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class ClientPersistenceAdapter {

    private static final Logger log = LoggerFactory.getLogger(ClientPersistenceAdapter.class);

    private final ClientInstanceRepository instanceRepository;
    private final ClientSubscriptionRepository subscriptionRepository;
    private final Clock clock;

    public ClientPersistenceAdapter(ClientInstanceRepository instanceRepository,
                                    ClientSubscriptionRepository subscriptionRepository,
                                    Clock appClock) {
        this.instanceRepository = instanceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.clock = appClock;
    }

    @Transactional
    public ClientInstance upsert(ClientInstance instance, List<ClientSubscription> subscriptions) {
        var entity = instanceRepository.findByServiceNameAndInstanceId(
                instance.serviceName(), instance.instanceId())
                .orElseGet(ClientInstanceEntity::new);

        if (entity.getId() == null) {
            entity.setPublicId(UlidCreator.getMonotonicUlid().toString());
            entity.setRegisteredAt(Instant.now(clock));
        }

        entity.setServiceName(instance.serviceName());
        entity.setInstanceId(instance.instanceId());
        entity.setPodName(instance.podName());
        entity.setNamespace(instance.namespace());
        entity.setCallbackUrl(instance.callbackUrl());
        entity.setStatus(ClientInstanceStatus.ACTIVE.name());

        var saved = saveInstanceResilient(instance, entity);

        subscriptionRepository.deleteAllByClientInstanceId(saved.getId());

        for (var subscription : subscriptions) {
            var subEntity = new ClientSubscriptionEntity();
            subEntity.setPublicId(UlidCreator.getMonotonicUlid().toString());
            subEntity.setClientInstanceId(saved.getId());
            subEntity.setToggleName(subscription.toggleName());
            subEntity.setConsumeMode(subscription.consumeMode().name());
            subEntity.setCreatedAt(Instant.now(clock));
            subscriptionRepository.save(subEntity);
        }

        return toDomain(saved);
    }

    private ClientInstanceEntity saveInstanceResilient(ClientInstance instance, ClientInstanceEntity entity) {
        try {
            return instanceRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // Handles concurrent register requests for the same serviceName+instanceId.
            var existing = instanceRepository.findByServiceNameAndInstanceId(
                    instance.serviceName(), instance.instanceId());

            if (existing.isEmpty()) {
                throw ex;
            }

            var recovered = existing.get();
            recovered.setPodName(instance.podName());
            recovered.setNamespace(instance.namespace());
            recovered.setCallbackUrl(instance.callbackUrl());
            recovered.setStatus(ClientInstanceStatus.ACTIVE.name());
            return instanceRepository.save(recovered);
        }
    }

    @Transactional
    public void deactivate(String serviceName, String instanceId) {
        var entity = instanceRepository.findByServiceNameAndInstanceId(serviceName, instanceId)
                .orElseThrow(() -> new ClientInstanceNotFoundException(serviceName, instanceId));

        entity.setStatus(ClientInstanceStatus.INACTIVE.name());
        var saved = instanceRepository.save(entity);

        log.info(
            "event=client_deregistered serviceName={} instanceId={} publicId={} namespace={} podName={} source=manual",
            saved.getServiceName(),
            saved.getInstanceId(),
            saved.getPublicId(),
            saved.getNamespace(),
            saved.getPodName());
    }

    @Transactional
    public void heartbeat(String serviceName, String instanceId) {
        var found = instanceRepository.findByServiceNameAndInstanceId(serviceName, instanceId);
        if (found.isEmpty()) {
            log.debug(
                    "event=client_heartbeat_rejected serviceName={} instanceId={} reason=not_found",
                    serviceName,
                    instanceId);
            throw new ClientInstanceNotFoundException(serviceName, instanceId);
        }

        var entity = found.get();

        if (ClientInstanceStatus.INACTIVE.name().equals(entity.getStatus())) {
            log.debug(
                    "event=client_heartbeat_rejected serviceName={} instanceId={} publicId={} namespace={} podName={} reason=inactive",
                    entity.getServiceName(),
                    entity.getInstanceId(),
                    entity.getPublicId(),
                    entity.getNamespace(),
                    entity.getPodName());
            throw new ClientInstanceInactiveException(serviceName, instanceId);
        }

        var now = Instant.now(clock);
        entity.setLastHeartbeatAt(now);
        entity.setLastSeenAt(now);
        var saved = instanceRepository.save(entity);

        log.debug(
                "event=client_heartbeat_accepted serviceName={} instanceId={} publicId={} namespace={} podName={} lastHeartbeatAt={}",
                saved.getServiceName(),
                saved.getInstanceId(),
                saved.getPublicId(),
                saved.getNamespace(),
                saved.getPodName(),
                saved.getLastHeartbeatAt());
    }

    @Transactional
    public int expireStaleInstances(Instant threshold) {
        var stale = instanceRepository.findAllByStatusAndLastHeartbeatAtBefore(
                ClientInstanceStatus.ACTIVE.name(), threshold);

        for (var entity : stale) {
            entity.setStatus(ClientInstanceStatus.INACTIVE.name());
            var saved = instanceRepository.save(entity);
            log.info(
                    "event=client_expired serviceName={} instanceId={} publicId={} namespace={} podName={} threshold={} source=heartbeat-timeout",
                    saved.getServiceName(),
                    saved.getInstanceId(),
                    saved.getPublicId(),
                    saved.getNamespace(),
                    saved.getPodName(),
                    threshold);
        }

        return stale.size();
    }

    public List<SubscriberView> findActiveSubscribersForToggle(String toggleName) {
        return subscriptionRepository.findByToggleName(toggleName).stream()
                .map(sub -> instanceRepository.findById(sub.getClientInstanceId()).orElse(null))
                .filter(inst -> inst != null && ClientInstanceStatus.ACTIVE.name().equals(inst.getStatus()))
                .map(inst -> new SubscriberView(inst.getId(), inst.getCallbackUrl()))
                .toList();
    }

    public record SubscriberView(Long clientInstanceId, String callbackUrl) {}

    private ClientInstance toDomain(ClientInstanceEntity entity) {
        return new ClientInstance(
                entity.getPublicId(),
                entity.getServiceName(),
                entity.getInstanceId(),
                entity.getPodName(),
                entity.getNamespace(),
                entity.getCallbackUrl(),
                ClientInstanceStatus.valueOf(entity.getStatus()),
                entity.getRegisteredAt());
    }
}
