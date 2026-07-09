package com.toggle.server.serviceauth.persistence;

import com.toggle.server.serviceauth.domain.ServiceApiKeyNotFoundException;
import com.toggle.server.serviceauth.application.ServiceApiKeyView;
import com.toggle.server.serviceauth.domain.ServiceApiKeyStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class ServiceApiKeyPersistenceAdapter {

    private final ServiceApiKeyRepository repository;

    public ServiceApiKeyPersistenceAdapter(ServiceApiKeyRepository repository) {
        this.repository = repository;
    }

    public ServiceApiKeyView create(String publicId, String serviceName, String name, String keyHash, Instant createdAt) {
        var entity = new ServiceApiKeyEntity();
        entity.setPublicId(publicId);
        entity.setServiceName(serviceName);
        entity.setName(name);
        entity.setKeyHash(keyHash);
        entity.setStatus(ServiceApiKeyStatus.ACTIVE.name());
        entity.setCreatedAt(createdAt);
        return toView(repository.save(entity));
    }

    public List<ServiceApiKeyView> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toView)
                .toList();
    }

    public Optional<ServiceApiKeyView> findActiveByHash(String keyHash) {
        return repository.findByKeyHashAndStatus(keyHash, ServiceApiKeyStatus.ACTIVE.name())
                .map(this::toView);
    }

    public void markUsed(String publicId, Instant usedAt) {
        repository.findByPublicId(publicId).ifPresent(entity -> {
            entity.setLastUsedAt(usedAt);
            repository.save(entity);
        });
    }

    public void revoke(String publicId, Instant revokedAt) {
        var entity = repository.findByPublicId(publicId)
                .orElseThrow(() -> new ServiceApiKeyNotFoundException(publicId));
        entity.setStatus(ServiceApiKeyStatus.REVOKED.name());
        entity.setRevokedAt(revokedAt);
        repository.save(entity);
    }

    private ServiceApiKeyView toView(ServiceApiKeyEntity entity) {
        return new ServiceApiKeyView(
                entity.getPublicId(),
                entity.getServiceName(),
                entity.getName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getLastUsedAt(),
                entity.getRevokedAt());
    }
}
