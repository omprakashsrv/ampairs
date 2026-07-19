-- Human-friendly, gap-free order number (e.g. "ECO-00001") issued via the sequence module at
-- checkout time. Distinct from ecom_order_ref (opaque, used as the stable API/lookup key) — this
-- is what the buyer sees and quotes when identifying/tracking their order. The same value is
-- copied onto the ingested management order (order.order_number) so both sides reference the
-- exact same number for a given order.
ALTER TABLE ecom_order ADD COLUMN order_number VARCHAR(50);
CREATE INDEX idx_ecom_order_order_number ON ecom_order (order_number);
