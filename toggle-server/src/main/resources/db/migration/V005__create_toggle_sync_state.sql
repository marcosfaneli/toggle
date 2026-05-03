CREATE TABLE toggle_sync_state
(
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    toggle_id                BIGINT       NOT NULL,
    client_instance_id       BIGINT       NOT NULL,
    target_version           BIGINT       NOT NULL,
    last_delivered_version   BIGINT       NULL,
    sync_status              VARCHAR(20)  NOT NULL,
    last_delivery_attempt_at TIMESTAMP    NULL,
    last_response_at         TIMESTAMP    NULL,
    retry_count              INT          NOT NULL DEFAULT 0,
    last_error               VARCHAR(512) NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_toggle_sync_state_toggle_instance UNIQUE (toggle_id, client_instance_id),
    CONSTRAINT fk_toggle_sync_state_toggle FOREIGN KEY (toggle_id) REFERENCES feature_toggle (id),
    CONSTRAINT fk_toggle_sync_state_client FOREIGN KEY (client_instance_id) REFERENCES client_instance (id)
);
