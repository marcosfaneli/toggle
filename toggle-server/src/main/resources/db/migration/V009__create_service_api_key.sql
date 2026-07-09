CREATE TABLE service_api_key (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_used_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_service_api_key_public_id UNIQUE (public_id),
    CONSTRAINT uk_service_api_key_key_hash UNIQUE (key_hash)
);

CREATE INDEX idx_service_api_key_service_name ON service_api_key (service_name);
CREATE INDEX idx_service_api_key_status ON service_api_key (status);
