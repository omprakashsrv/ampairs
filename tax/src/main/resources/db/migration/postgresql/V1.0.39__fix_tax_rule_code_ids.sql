-- =====================================================
-- Migration: Fix Missing tax_code_id in tax_rule table
-- Version: V1.0.39
-- =====================================================
-- Purpose: Populate empty tax_code_id fields in existing tax rules
--
-- Background:
-- - Tax rules created before auto-creation feature have empty tax_code_id
-- - tax_code_id should reference the workspace tax_code.uid
-- - Can match using tax_code string + owner_id
-- =====================================================

-- Update tax_code_id by matching tax_code string with workspace tax_code records
UPDATE tax_rule tr
SET tax_code_id = (
    SELECT tc.uid
    FROM tax_code tc
    WHERE tc.code = tr.tax_code
      AND tc.owner_id = tr.owner_id
      AND tc.is_active = true
    LIMIT 1
)
WHERE (tr.tax_code_id = '' OR tr.tax_code_id IS NULL)
  AND EXISTS (
      SELECT 1
      FROM tax_code tc
      WHERE tc.code = tr.tax_code
        AND tc.owner_id = tr.owner_id
        AND tc.is_active = true
  );

-- Log the results for monitoring
DO $$
DECLARE
    total_updated INTEGER;
    remaining_empty INTEGER;
BEGIN
    -- Count remaining empty tax_code_id
    SELECT COUNT(*) INTO remaining_empty
    FROM tax_rule
    WHERE tax_code_id = '' OR tax_code_id IS NULL;

    -- Get total count
    SELECT COUNT(*) INTO total_updated
    FROM tax_rule
    WHERE tax_code_id IS NOT NULL AND tax_code_id != '';

    RAISE NOTICE 'Tax Rule Code ID Fix Complete';
    RAISE NOTICE 'Fixed tax rules: %', total_updated;
    RAISE NOTICE 'Remaining empty: %', remaining_empty;
END $$;
