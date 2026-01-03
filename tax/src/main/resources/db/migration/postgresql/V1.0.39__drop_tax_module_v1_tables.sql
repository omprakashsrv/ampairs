-- Tax Module V1 Cleanup Migration (PostgreSQL)
-- Version: V1.0.39
-- Description: Drop V1 tax module tables after migration to V2 architecture
-- Author: Claude Code
-- Date: 2025-01-09

-- =====================================================
-- Drop V1 Tables
-- =====================================================

-- Drop dependent tables first (ones with foreign keys)
DROP TABLE IF EXISTS tax_rates CASCADE;
DROP TABLE IF EXISTS hsn_codes CASCADE;
DROP TABLE IF EXISTS business_types CASCADE;

-- =====================================================
-- End of Tax Module V1 Cleanup Migration
-- =====================================================
