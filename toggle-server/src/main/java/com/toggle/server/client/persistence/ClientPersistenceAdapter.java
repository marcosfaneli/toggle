package com.toggle.server.client.persistence;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceNotFoundException;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.ClientSubscription;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ClientPersistenceAdapter {

    private final ClientInstanceRepository instanceRepository;
    private final ClientSubscriptionRepository subscriptionRepository;

    public ClientPersistenceAdapter(ClientInstanceRepository instanceRepository,
                                    ClientSubscriptionRepository subscriptionRepository) {
        this.instanceRepository = instanceRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public ClientInstance upsert(ClientInstance instance, List<ClientSubscription> subscriptions) {
        var entity = instanceRepository.findByServiceNameAndInstanceId(
                instance.serviceName(), instance.instanceId())
                .orElseGet(ClientInstanceEntity::new);

        boolean isNew = entity.getId() == null;

        if (isNew) {
            entity.setPublicId(UlidCreator.getMonotonicUlid().toString());
            entity.setRegisteredAt(LocalDateTime.now());
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
            subEntity.setCreatedAt(LocalDateTime.now());
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
