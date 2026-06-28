-- Notification queue: columns for the communication module's structured dispatch + billing attribution (MySQL)
ALTER TABLE notification_queue
    ADD COLUMN subject VARCHAR(500),
    ADD COLUMN source_module VARCHAR(50),
    ADD COLUMN source_ref VARCHAR(200),
    ADD COLUMN credential_uid VARCHAR(200),
    ADD COLUMN billing_mode VARCHAR(20),
    ADD INDEX idx_notification_queue_source_ref (source_ref);
