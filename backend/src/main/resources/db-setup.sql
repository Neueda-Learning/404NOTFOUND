-- Transaction Monitoring System - MySQL Database Setup
-- Run this script to create the database before starting the application

CREATE DATABASE IF NOT EXISTS transaction_monitoring
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE transaction_monitoring;

-- The application uses Spring Boot JPA with ddl-auto=update,
-- so tables will be created automatically on first startup.
-- This script just ensures the database exists.

-- Optional: Create a dedicated user for the application
-- CREATE USER IF NOT EXISTS 'framl_user'@'localhost' IDENTIFIED BY 'framl_pass';
-- GRANT ALL PRIVILEGES ON transaction_monitoring.* TO 'framl_user'@'localhost';
-- FLUSH PRIVILEGES;
