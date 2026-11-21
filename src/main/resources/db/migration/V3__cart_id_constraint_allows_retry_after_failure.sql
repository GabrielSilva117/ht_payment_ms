-- Plain UNIQUE(cart_id) blocked retrying a cart forever after one FAILED attempt.
-- Replace it with a partial unique index: at most one non-FAILED row per cart
-- (PENDING/PROCESSING/SUCCEEDED all count as "live"), so a cart can have many
-- FAILED rows (attempt history) but never two concurrent live attempts, and
-- never a new attempt once one has SUCCEEDED.
ALTER TABLE payments DROP CONSTRAINT uk_payments_cart_id;

CREATE UNIQUE INDEX uk_payments_cart_active ON payments (cart_id) WHERE status <> 'FAILED';
