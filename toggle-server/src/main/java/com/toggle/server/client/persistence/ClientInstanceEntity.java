package com.toggle.server.client.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_instance")
class ClientInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "instance_id", nullable = false)
    private String instanceId;

    @Column(name = "pod_name", nullable = false)
    private String podName;

    @Column(name = "namespace", nullable = false)
    private String namespace;

    @Column(name = "callback_url", nullable = false, length = 512)
    private String callbackUrl;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    String getPublicId() { return publicId; }
    void setPublicId(String publicId) { this.publicId = publicId; }

    String getServiceName() { return serviceName; }
    void setServiceName(String serviceName) { this.serviceName = serviceName; }

    String getInstanceId() { return instanceId; }
    void setInstanceId(String instanceId) { this.instanceId = instanceId; }

    String getPodName() { return podName; }
    void setPodName(String podName) { this.podName = podName; }

    String getNamespace() { return namespace; }
    void setNamespace(String namespace) { this.namespace = namespace; }

    String getCallbackUrl() { return callbackUrl; }
    void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }

    LocalDateTime getRegisteredAt() { return registeredAt; }
    void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    LocalDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
    void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }

    LocalDateTime getLastSeenAt() { return lastSeenAt; }
    void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
