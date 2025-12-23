CREATE TABLE IF NOT EXISTS users
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50) UNIQUE  NOT NULL,
    password   VARCHAR(255)        NOT NULL,
    email      VARCHAR(100) UNIQUE NOT NULL,
    role       VARCHAR(20)         NOT NULL,
    enabled    BOOLEAN             NOT NULL DEFAULT true,
    created_at TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bank_cards
(
    id                    BIGSERIAL PRIMARY KEY,
    card_number_encrypted VARCHAR(255) NOT NULL,
    card_number_masked    VARCHAR(20)  NOT NULL,
    card_holder           VARCHAR(100) NOT NULL,
    expiry_date           DATE         NOT NULL,
    cvv_encrypted         VARCHAR(255) NOT NULL,
    status                VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    balance               DECIMAL(15, 2) DEFAULT 0.00,
    user_id               BIGINT REFERENCES users (id),
    created_at            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS card_transactions
(
    id               BIGSERIAL PRIMARY KEY,
    from_card_id     BIGINT REFERENCES bank_cards (id),
    to_card_id       BIGINT REFERENCES bank_cards (id),
    amount           DECIMAL(15, 2) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status           VARCHAR(20)    NOT NULL
);