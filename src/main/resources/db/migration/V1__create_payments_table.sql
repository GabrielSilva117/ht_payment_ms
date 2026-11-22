CREATE TABLE payments (
    id                 UUID PRIMARY KEY,
    cart_id            UUID NOT NULL,
    amount             NUMERIC(19,4) NOT NULL,
    currency           VARCHAR(3) NOT NULL,
    payment_type       VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    gateway_reference  VARCHAR(255),
    failure_reason     VARCHAR(1000),
    card_last_4        VARCHAR(4),
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payments_cart_id UNIQUE (cart_id)
);

CREATE INDEX idx_payments_status ON payments (status);
