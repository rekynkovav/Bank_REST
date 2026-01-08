UPDATE users SET enabled = true WHERE enabled IS NULL;

ALTER TABLE users
    ALTER COLUMN enabled SET NOT NULL,
    ALTER COLUMN enabled SET DEFAULT true;

INSERT INTO users (username, password, email, role, enabled)
VALUES ('admin', 'encrypted_password', 'admin@bank.com', 'ADMIN', true)
ON CONFLICT (username) DO UPDATE SET
                                     password = EXCLUDED.password,
                                     email = EXCLUDED.email,
                                     role = EXCLUDED.role,
                                     enabled = EXCLUDED.enabled;