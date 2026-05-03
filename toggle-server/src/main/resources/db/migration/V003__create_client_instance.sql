CREATE TABLE client_instance
(
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    public_id          VARCHAR(36)   NOT NULL,
    service_name       VARCHAR(255)  NOT NULL,
    instance_id        VARCHAR(255)  NOT NULL,
    pod_name           VARCHAR(255)  NOT NULL,
    namespace          VARCHAR(255)  NOT NULL,
    callback_url       VARCHAR(512)  NOT NULL,
    status             VARCHAR(16)   NOT NULL,
    registered_at      TIMESTAMP     NOT NULL,
    last_heartbeat_at  TIMESTAMP     NULL,
    last_seen_at       TIMESTAMP     NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_client_instance_public_id UNIQUE (public_id),
    CONSTRAINT uq_client_instance_service_instance UNIQUE (service_name, instance_id)
);
