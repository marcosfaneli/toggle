package com.toggle.server.serviceauth.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "service_api_key")
class ServiceApiKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    String getPublicId() { return publicId; }
    void setPublicId(String publicId) { this.publicId = publicId; }

    String getServiceName() { return serviceName; }
    void setServiceName(String serviceName) { this.serviceName = serviceName; }

    String getName() { return name; }
    void setName(String name) { this.name = name; }

    String getKeyHash() { return keyHash; }
    void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }

    Instant getCreatedAt() { return createdAt; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    Instant getLastUsedAt() { return lastUsedAt; }
    void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    Instant getRevokedAt() { return revokedAt; }
    void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
