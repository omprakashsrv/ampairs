-- Notification Module Migration (MySQL)
-- Version: 1.0.103
-- Description: Add push-notification columns (title, data_payload) to notification_queue
--              so FCM rows can carry a title and structured data payload.

ALTER TABLE notification_queue ADD COLUMN title VARCHAR(255) NULL;
ALTER TABLE notification_queue ADD COLUMN data_payload TEXT NULL;
