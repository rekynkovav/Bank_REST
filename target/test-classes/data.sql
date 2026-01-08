INSERT INTO users (username, password, email, role, enabled)
VALUES ('testuser', '$2a$10$SomeHash...', 'test@bank.com', 'USER', true),
       ('admin', '$2a$10$AdminHash...', 'admin@bank.com', 'ADMIN', true);