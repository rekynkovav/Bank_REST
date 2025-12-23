CREATE TABLE IF NOT EXISTS audit_logs
(
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   BIGINT,
    details     TEXT,
    user_id     BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_user_id') THEN
            CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_action') THEN
            CREATE INDEX idx_audit_logs_action ON audit_logs (action);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_entity_type') THEN
            CREATE INDEX idx_audit_logs_entity_type ON audit_logs (entity_type);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_created_at') THEN
            CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_audit_logs_entity_composite') THEN
            CREATE INDEX idx_audit_logs_entity_composite ON audit_logs (entity_type, entity_id);
        END IF;
    END
$$;