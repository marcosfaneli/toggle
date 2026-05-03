package com.toggle.server.client.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceInactiveException;
import com.toggle.server.client.domain.ClientInstanceNotFoundException;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.ClientSubscription;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class ClientPersistenceAdapter {

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

        boolean isNew = entity.getId() == null;

        if (isNew) {
            entity.setPublicId(UlidCreator.getMonotonicUlid().toString());
            entity.setRegisteredAt(Instant.now(clock));
        }

        entity.setServiceName(instance.serviceName());
        entity.setInstanceId(instance.instanceId());
        entity.setPodName(instance.podName());
        entity.setNamespace(instance.namespace());
        entity.setCallbackUrl(instance.callbackUrl());
        entity.setStatus(ClientInstanceStatus.ACTIVE.name());

        var saved = instanceRepository.save(entity);

        if (!isNew) {
            subscriptionRepository.deleteAllByClientInstanceId(saved.getId());
        }

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

    @Transactional
    public void deactivate(String serviceName, String instanceId) {
        var entity = instanceRepository.findByServiceNameAndInstanceId(serviceName, instanceId)
                .orElseThrow(() -> new ClientInstanceNotFoundException(serviceName, instanceId));

        entity.setStatus(ClientInstanceStatus.INACTIVE.name());
        instanceRepository.save(entity);
    }

    @Transactional
    public void heartbeat(String serviceName, String instanceId) {
        var entity = instanceRepository.findByServiceNameAndInstanceId(serviceName, instanceId)
                .orElseThrow(() -> new ClientInstanceNotFoundException(serviceName, instanceId));

        if (ClientInstanceStatus.INACTIVE.name().equals(entity.getStatus())) {
            throw new ClientInstanceInactiveException(serviceName, instanceId);
        }

        var now = Instant.now(clock);
        entity.setLastHeartbeatAt(now);
        entity.setLastSeenAt(now);
        instanceRepository.save(entity);
    }

    @Transactional
    public int expireStaleInstances(Instant threshold) {
        var stale = instanceRepository.findAllByStatusAndLastHeartbeatAtBefore(
                ClientInstanceStatus.ACTIVE.name(), threshold);

        for (var entity : stale) {
            entity.setStatus(ClientInstanceStatus.INACTIVE.name());
            instanceRepository.save(entity);
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
