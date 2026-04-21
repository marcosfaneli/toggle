package com.toggle.server.toggle.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_toggle")
class ToggleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner_service_name", nullable = false)
    private String ownerServiceName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    String getPublicId() { return publicId; }
    void setPublicId(String publicId) { this.publicId = publicId; }

    String getName() { return name; }
    void setName(String name) { this.name = name; }

    String getOwnerServiceName() { return ownerServiceName; }
    void setOwnerServiceName(String ownerServiceName) { this.ownerServiceName = ownerServiceName; }

    boolean isEnabled() { return enabled; }
    void setEnabled(boolean enabled) { this.enabled = enabled; }

    Long getVersion() { return version; }
    void setVersion(Long version) { this.version = version; }

    LocalDateTime getUpdatedAt() { return updatedAt; }
    void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
