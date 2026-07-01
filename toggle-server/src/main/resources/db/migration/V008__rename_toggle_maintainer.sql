ALTER TABLE feature_toggle
    RENAME COLUMN owner_service_name TO maintainer;

CREATE INDEX idx_feature_toggle_maintainer
    ON feature_toggle (maintainer);
