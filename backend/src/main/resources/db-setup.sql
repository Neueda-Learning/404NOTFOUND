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
-- ALTER TABLE alerts MODIFY COLUMN alert_id VARCHAR(40) NOT NULL;
-- ALTER TABLE alerts ADD COLUMN deduplication_key VARCHAR(160) NOT NULL;
-- ALTER TABLE alerts ADD CONSTRAINT uk_alert_deduplication_key UNIQUE (deduplication_key);

-- Optional performance indexes for high-traffic environments:
-- Alerts query/index paths
-- CREATE INDEX idx_alert_status_created ON alerts(status, created_at);
-- CREATE INDEX idx_alert_severity_created ON alerts(severity, created_at);
-- CREATE INDEX idx_alert_account_created ON alerts(account_id, created_at);
-- CREATE INDEX idx_alert_primary_tx ON alerts(primary_transaction_id);

-- Transactions query/index paths
-- CREATE INDEX idx_tx_account_status_type_time ON transactions(account_id, status, type, transaction_time);
-- CREATE INDEX idx_tx_account_currency_status_type_time ON transactions(account_id, currency, status, type, transaction_time);
-- CREATE INDEX idx_tx_account_payee_status_time ON transactions(account_id, payee_id, status, transaction_time);

-- Alert transactions join/index paths
-- CREATE INDEX idx_alert_tx_transaction_id ON alert_transactions(transaction_id);
-- CREATE INDEX idx_alert_tx_alert_id ON alert_transactions(alert_id);
-- CREATE INDEX idx_alert_tx_transaction_alert ON alert_transactions(transaction_id, alert_id);

-- Optional: Create a dedicated user for the application
-- CREATE USER IF NOT EXISTS 'framl_user'@'localhost' IDENTIFIED BY 'framl_pass';
-- GRANT ALL PRIVILEGES ON transaction_monitoring.* TO 'framl_user'@'localhost';
-- FLUSH PRIVILEGES;
