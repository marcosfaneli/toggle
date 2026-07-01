package com.toggle.server.toggle.persistence;

import jakarta.persistence.*;

import java.time.Instant;

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

    @Column(name = "maintainer", nullable = false)
    private String maintainer;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "toggle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ToggleValueEntity value;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    String getPublicId() { return publicId; }
    void setPublicId(String publicId) { this.publicId = publicId; }

    String getName() { return name; }
    void setName(String name) { this.name = name; }

    String getMaintainer() { return maintainer; }
    void setMaintainer(String maintainer) { this.maintainer = maintainer; }

    boolean isEnabled() { return enabled; }
    void setEnabled(boolean enabled) { this.enabled = enabled; }

    Long getVersion() { return version; }
    void setVersion(Long version) { this.version = version; }

    Instant getUpdatedAt() { return updatedAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    ToggleValueEntity getValue() { return value; }
    void setValue(ToggleValueEntity value) { this.value = value; }
}
