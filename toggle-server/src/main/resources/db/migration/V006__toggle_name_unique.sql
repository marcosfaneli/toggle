ALTER TABLE feature_toggle
    DROP INDEX uq_feature_toggle_name_owner,
    ADD CONSTRAINT uq_feature_toggle_name UNIQUE (name);
