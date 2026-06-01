-- Allow duplicate GST numbers across customers (e.g. same business, multiple contacts)
DROP INDEX uk_customer_gst ON customer;
