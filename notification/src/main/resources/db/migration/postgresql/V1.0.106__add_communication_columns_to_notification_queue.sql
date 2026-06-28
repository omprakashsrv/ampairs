-- Notification queue: columns for the communication module's structured dispatch + billing attribution (PostgreSQL)
ALTER TABLE notification_queue ADD COLUMN subject VARCHAR(500);
ALTER TABLE notification_queue ADD COLUMN source_module VARCHAR(50);
ALTER TABLE notification_queue ADD COLUMN source_ref VARCHAR(200);
ALTER TABLE notification_queue ADD COLUMN credential_uid VARCHAR(200);
ALTER TABLE notification_queue ADD COLUMN billing_mode VARCHAR(20);
CREATE INDEX idx_notification_queue_source_ref ON notification_queue(source_ref);
