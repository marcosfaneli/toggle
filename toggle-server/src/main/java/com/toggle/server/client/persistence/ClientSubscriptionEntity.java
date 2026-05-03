package com.toggle.server.client.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "client_toggle_subscription")
class ClientSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "client_instance_id", nullable = false)
    private Long clientInstanceId;

    @Column(name = "toggle_name", nullable = false)
    private String toggleName;

    @Column(name = "consume_mode", nullable = false, length = 16)
    private String consumeMode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    String getPublicId() { return publicId; }
    void setPublicId(String publicId) { this.publicId = publicId; }

    Long getClientInstanceId() { return clientInstanceId; }
    void setClientInstanceId(Long clientInstanceId) { this.clientInstanceId = clientInstanceId; }

    String getToggleName() { return toggleName; }
    void setToggleName(String toggleName) { this.toggleName = toggleName; }

    String getConsumeMode() { return consumeMode; }
    void setConsumeMode(String consumeMode) { this.consumeMode = consumeMode; }

    Instant getCreatedAt() { return createdAt; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
