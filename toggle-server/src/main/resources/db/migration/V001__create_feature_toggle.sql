CREATE TABLE feature_toggle
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    public_id         VARCHAR(36)  NOT NULL,
    name              VARCHAR(255) NOT NULL,
    owner_service_name VARCHAR(255) NOT NULL,
    enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    version           BIGINT       NOT NULL DEFAULT 1,
    updated_at        TIMESTAMP    NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_feature_toggle_public_id UNIQUE (public_id),
    CONSTRAINT uq_feature_toggle_name_owner UNIQUE (name, owner_service_name)
);
