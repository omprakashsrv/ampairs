-- Notification Module Migration (PostgreSQL)
-- Version: 1.0.99
-- Description: Add push-notification columns (title, data_payload) to notification_queue
--              so FCM rows can carry a title and structured data payload.

ALTER TABLE notification_queue ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE notification_queue ADD COLUMN IF NOT EXISTS data_payload TEXT;

COMMENT ON COLUMN notification_queue.title IS 'Optional title for push (FCM) rows; null for SMS';
COMMENT ON COLUMN notification_queue.data_payload IS 'Optional JSON key/value payload for push notifications';
