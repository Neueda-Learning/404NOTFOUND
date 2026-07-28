-- Transaction Monitoring System - MySQL Database Setup
-- Run this script to create the database before starting the application

CREATE DATABASE IF NOT EXISTS transaction_monitoring
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE transaction_monitoring;

-- The application uses Spring Boot JPA with ddl-auto=update,
-- so tables will be created automatically on first startup.
-- This script just ensures the database exists.

-- Optional schema hardening for existing environments:
-- Ensure alerts table uses UUID-length IDs and unique deduplication key.
-- ALTER TABLE alerts MODIFY COLUMN alert_id VARCHAR(36) NOT NULL;
-- ALTER TABLE alerts ADD COLUMN deduplication_key VARCHAR(160) NOT NULL;
-- ALTER TABLE alerts ADD CONSTRAINT uk_alert_deduplication_key UNIQUE (deduplication_key);

-- Optional: Create a dedicated user for the application
-- CREATE USER IF NOT EXISTS 'framl_user'@'localhost' IDENTIFIED BY 'framl_pass';
-- GRANT ALL PRIVILEGES ON transaction_monitoring.* TO 'framl_user'@'localhost';
-- FLUSH PRIVILEGES;
