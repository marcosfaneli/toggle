CREATE TABLE feature_toggle_value
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    toggle_id  BIGINT       NOT NULL,
    value_type VARCHAR(16)  NOT NULL,
    value_raw  VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_feature_toggle_value_toggle_id UNIQUE (toggle_id),
    CONSTRAINT fk_feature_toggle_value_toggle FOREIGN KEY (toggle_id) REFERENCES feature_toggle (id)
);
