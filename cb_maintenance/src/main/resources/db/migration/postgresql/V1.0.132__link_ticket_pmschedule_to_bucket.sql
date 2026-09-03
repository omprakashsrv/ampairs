-- cb_maintenance Module Migration (PostgreSQL)
-- Version: 1.0.132
-- Description: link ticket AND pm_schedule -> ticket_bucket (exact taxonomy leaf) and record the
--              PM schedule's department, so reports can resolve the full classification of each
--              ticket / PM.
-- Dependencies: V1.0.130 (ticket, pm_schedule), V1.0.131 (ticket_bucket)

ALTER TABLE ticket ADD COLUMN ticket_bucket_id VARCHAR(200);
CREATE INDEX idx_cb_ticket_bucket ON ticket(ticket_bucket_id);

ALTER TABLE pm_schedule ADD COLUMN department VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE pm_schedule ADD COLUMN ticket_bucket_id VARCHAR(200);
CREATE INDEX idx_cb_pm_schedule_bucket ON pm_schedule(ticket_bucket_id);
