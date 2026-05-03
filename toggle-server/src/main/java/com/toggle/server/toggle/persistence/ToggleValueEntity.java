package com.toggle.server.toggle.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "feature_toggle_value")
class ToggleValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toggle_id", nullable = false, unique = true)
    private ToggleEntity toggle;

    @Column(name = "value_type", nullable = false, length = 16)
    private String valueType;

    @Column(name = "value_raw", nullable = false, length = 255)
    private String valueRaw;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    Long getId() { return id; }
    void setId(Long id) { this.id = id; }

    ToggleEntity getToggle() { return toggle; }
    void setToggle(ToggleEntity toggle) { this.toggle = toggle; }

    String getValueType() { return valueType; }
    void setValueType(String valueType) { this.valueType = valueType; }

    String getValueRaw() { return valueRaw; }
    void setValueRaw(String valueRaw) { this.valueRaw = valueRaw; }

    Instant getUpdatedAt() { return updatedAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
