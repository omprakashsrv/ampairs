-- ============================================
-- Fix Missing tax_code_id in tax_rule table
-- ============================================
-- Purpose: Populate empty tax_code_id fields by matching tax_code string
--          with workspace tax_code records
--
-- Issue: Old tax rules have empty tax_code_id but valid tax_code string
-- Solution: Match by tax_code + owner_id and populate the FK
-- ============================================

-- Step 1: View affected records
SELECT
    tr.uid as rule_id,
    tr.tax_code_id as current_tax_code_id,
    tr.tax_code as tax_code_string,
    tr.owner_id,
    tc.uid as workspace_tax_code_uid,
    tc.code as workspace_code
FROM tax_rule tr
LEFT JOIN tax_code tc ON
    tc.code = tr.tax_code
    AND tc.owner_id = tr.owner_id
    AND tc.is_active = true
WHERE tr.tax_code_id = '' OR tr.tax_code_id IS NULL;

-- Step 2: Update tax_code_id by matching tax_code string with workspace tax_code
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

-- Step 3: Verify the fix
SELECT
    COUNT(*) as total_rules,
    COUNT(CASE WHEN tax_code_id = '' OR tax_code_id IS NULL THEN 1 END) as rules_with_empty_id,
    COUNT(CASE WHEN tax_code_id != '' AND tax_code_id IS NOT NULL THEN 1 END) as rules_with_valid_id
FROM tax_rule;

-- Step 4: Show any remaining unmatched rules (orphaned rules with no workspace tax code)
SELECT
    tr.uid,
    tr.tax_code,
    tr.tax_code_type,
    tr.owner_id,
    'No matching workspace tax_code found' as issue
FROM tax_rule tr
WHERE (tr.tax_code_id = '' OR tr.tax_code_id IS NULL)
  AND NOT EXISTS (
      SELECT 1
      FROM tax_code tc
      WHERE tc.code = tr.tax_code
        AND tc.owner_id = tr.owner_id
  );
