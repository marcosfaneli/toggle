package com.toggle.server.delivery.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "toggle_sync_state")
class ToggleSyncStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "toggle_id", nullable = false)
    private Long toggleId;

    @Column(name = "client_instance_id", nullable = false)
    private Long clientInstanceId;

    @Column(name = "target_version", nullable = false)
    private Long targetVersion;

    @Column(name = "last_delivered_version")
    private Long lastDeliveredVersion;

    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus;

    @Column(name = "last_delivery_attempt_at")
    private Instant lastDeliveryAttemptAt;

    @Column(name = "last_response_at")
    private Instant lastResponseAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error", length = 512)
    private String lastError;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    Long getToggleId() { return toggleId; }
    void setToggleId(Long toggleId) { this.toggleId = toggleId; }

    Long getClientInstanceId() { return clientInstanceId; }
    void setClientInstanceId(Long clientInstanceId) { this.clientInstanceId = clientInstanceId; }

    Long getTargetVersion() { return targetVersion; }
    void setTargetVersion(Long targetVersion) { this.targetVersion = targetVersion; }

    Long getLastDeliveredVersion() { return lastDeliveredVersion; }
    void setLastDeliveredVersion(Long lastDeliveredVersion) { this.lastDeliveredVersion = lastDeliveredVersion; }

    String getSyncStatus() { return syncStatus; }
    void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    Instant getLastDeliveryAttemptAt() { return lastDeliveryAttemptAt; }
    void setLastDeliveryAttemptAt(Instant lastDeliveryAttemptAt) { this.lastDeliveryAttemptAt = lastDeliveryAttemptAt; }

    Instant getLastResponseAt() { return lastResponseAt; }
    void setLastResponseAt(Instant lastResponseAt) { this.lastResponseAt = lastResponseAt; }

    int getRetryCount() { return retryCount; }
    void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    String getLastError() { return lastError; }
    void setLastError(String lastError) { this.lastError = lastError; }
}
