-- Allow duplicate GST numbers across customers (e.g. same business, multiple contacts)
DROP INDEX IF EXISTS uk_customer_gst;
