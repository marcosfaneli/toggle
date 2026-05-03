CREATE TABLE client_toggle_subscription
(
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    public_id          VARCHAR(36)   NOT NULL,
    client_instance_id BIGINT        NOT NULL,
    toggle_name        VARCHAR(255)  NOT NULL,
    consume_mode       VARCHAR(16)   NOT NULL,
    created_at         TIMESTAMP     NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_client_toggle_subscription_public_id UNIQUE (public_id),
    CONSTRAINT fk_client_toggle_subscription_instance FOREIGN KEY (client_instance_id) REFERENCES client_instance (id)
);
