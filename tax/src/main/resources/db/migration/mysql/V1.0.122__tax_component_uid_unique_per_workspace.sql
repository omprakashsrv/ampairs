-- Tax component uid uniqueness is per-workspace, not global (MySQL).
--
-- tax_component is a tenant-scoped entity (@TenantId owner_id) whose rows deliberately reuse the
-- master component uid (e.g. COMP_CGST_9) across workspaces. The subscribe flow de-dups with a
-- @TenantId-filtered findByUid, so workspace B never sees workspace A's COMP_CGST_9 and tries to
-- insert its own copy. The old global UNIQUE(uid) then rejected it with a duplicate-key violation,
-- so only the first workspace to subscribe a given rate could add that tax code.
--
-- Fix: drop the global unique on uid (the inline column UNIQUE creates an index named `uid`) and
-- make (owner_id, uid) unique instead. Existing rows are unaffected — a globally-unique set is
-- already unique per owner.
ALTER TABLE tax_component DROP INDEX uid;
ALTER TABLE tax_component ADD CONSTRAINT uk_tax_component_owner_uid UNIQUE (owner_id, uid);
