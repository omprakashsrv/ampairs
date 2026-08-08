-- Fix fk_invoice_order_ref: it referenced customer_order(ref_id), a column that is never
-- populated anywhere in the codebase (Order.refId has no assignment). Both Order.toInvoice()
-- and the client-facing InvoiceUpdateRequest.orderRefId populate invoice.order_ref_id with the
-- order's uid, so the FK must target customer_order(uid) instead — otherwise every order->invoice
-- conversion and every client-synced invoice carrying an order ref fails with a FK violation.
ALTER TABLE invoice DROP CONSTRAINT fk_invoice_order_ref;

ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_order_ref FOREIGN KEY (order_ref_id) REFERENCES customer_order (uid) ON DELETE SET NULL;
