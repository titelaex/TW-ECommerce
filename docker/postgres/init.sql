-- Ruleaza o singura data, la prima pornire a containerului (volum gol).
-- Dupa o modificare aici: docker compose down -v, apoi docker compose up -d (sterge datele).

CREATE DATABASE payments_db;
CREATE DATABASE orders_db;

\connect payments_db

-- Numele coloanelor urmeaza conventia Spring/JPA (orderId -> order_id), ca entitatea Payment sa se mapeze direct.
CREATE TABLE payments (
    id                    UUID           PRIMARY KEY,
    order_id              VARCHAR(255)   NOT NULL,
    user_id               VARCHAR(255)   NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    status                VARCHAR(20)    NOT NULL
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
    failure_reason        VARCHAR(255),
    transaction_reference VARCHAR(255),
    version               BIGINT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Unic: o singura plata per comanda, ca doua cereri simultane sa nu poata debita de doua ori.
CREATE UNIQUE INDEX uq_payments_order_id ON payments (order_id);

-- Pentru istoricul platilor unui utilizator (cele mai noi primele).
CREATE INDEX idx_payments_user_created ON payments (user_id, created_at DESC);
