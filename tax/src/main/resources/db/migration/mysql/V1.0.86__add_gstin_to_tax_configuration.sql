-- Workspace tax registration (GSTIN). Its first two digits are the supplier's state code, which
-- drives the intra/inter-state (CGST+SGST vs IGST) scenario for orders and invoices.
ALTER TABLE tax_configuration ADD COLUMN gstin VARCHAR(20);
