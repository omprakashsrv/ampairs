-- Backfill missing tax_code_id on tax_rule rows.
-- Old tax rules were created with an empty tax_code_id but carry a valid tax_code string;
-- match by (tax_code, owner_id) against the workspace's active tax_code records and
-- populate the FK. Idempotent: only touches rows where tax_code_id is still empty/null
-- and a matching active tax_code exists. Rows with no match are intentionally left
-- untouched (orphaned rules; no safe automatic resolution).
-- Replaces the ad-hoc repo-root script fix-tax-rule-code-ids.sql.

UPDATE tax_rule tr
SET tr.tax_code_id = (
    SELECT tc.uid
    FROM tax_code tc
    WHERE tc.code = tr.tax_code
      AND tc.owner_id = tr.owner_id
      AND tc.is_active = TRUE
    LIMIT 1
)
WHERE (tr.tax_code_id = '' OR tr.tax_code_id IS NULL)
  AND EXISTS (
      SELECT 1
      FROM tax_code tc
      WHERE tc.code = tr.tax_code
        AND tc.owner_id = tr.owner_id
        AND tc.is_active = TRUE
  );
