-- Transaction Monitoring & Alerts Dashboard
-- MySQL 8.0 reference DDL, version 3.0
-- All DATETIME values are stored in UTC. Application startup must execute: SET time_zone = '+00:00';
-- Monetary values use DECIMAL(19,4); the service validates ISO 4217 minor units.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS transaction_monitoring
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE transaction_monitoring;

CREATE TABLE accounts (
  id                    VARCHAR(64) PRIMARY KEY,
  display_name          VARCHAR(200) NOT NULL,
  masked_account_number VARCHAR(64) NOT NULL,
  country_code          CHAR(2) NULL,
  risk_level            ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'LOW',
  status                ENUM('ACTIVE','INACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT chk_account_country CHECK (country_code IS NULL OR country_code = UPPER(country_code))
) ENGINE=InnoDB;

CREATE TABLE payees (
  id                    VARCHAR(64) PRIMARY KEY,
  display_name          VARCHAR(200) NOT NULL,
  masked_account_number VARCHAR(64) NULL,
  country_code          CHAR(2) NULL,
  status                ENUM('ACTIVE','INACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT chk_payee_country CHECK (country_code IS NULL OR country_code = UPPER(country_code))
) ENGINE=InnoDB;

CREATE TABLE rules (
  id                    VARCHAR(64) PRIMARY KEY,
  name                  VARCHAR(200) NOT NULL,
  type                  ENUM('AMOUNT_THRESHOLD','VELOCITY','NEW_PAYEE','DAILY_LIMIT') NOT NULL,
  severity              ENUM('LOW','MEDIUM','HIGH') NOT NULL,
  active                BOOLEAN NOT NULL DEFAULT TRUE,
  config                JSON NOT NULL,
  version               INT UNSIGNED NOT NULL DEFAULT 1,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT chk_rule_version CHECK (version > 0),
  CONSTRAINT chk_rule_schema_version CHECK (JSON_EXTRACT(config, '$.schemaVersion') IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE rule_history (
  id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  rule_id               VARCHAR(64) NOT NULL,
  version               INT UNSIGNED NOT NULL,
  action                ENUM('CREATED','UPDATED','ACTIVATED','DEACTIVATED') NOT NULL,
  previous_snapshot     JSON NULL,
  new_snapshot          JSON NOT NULL,
  changed_fields        JSON NOT NULL,
  changed_by            VARCHAR(128) NOT NULL DEFAULT 'SYSTEM',
  changed_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  reason                VARCHAR(1000) NOT NULL,
  correlation_id        VARCHAR(128) NOT NULL,
  CONSTRAINT fk_rule_history_rule FOREIGN KEY (rule_id) REFERENCES rules(id),
  CONSTRAINT uq_rule_history_version UNIQUE (rule_id, version),
  INDEX idx_rule_history_timeline (rule_id, version)
) ENGINE=InnoDB;

CREATE TABLE transactions (
  id                       VARCHAR(64) PRIMARY KEY,
  account_id               VARCHAR(64) NOT NULL,
  payee_id                 VARCHAR(64) NOT NULL,
  amount                   DECIMAL(19,4) NOT NULL,
  currency                 CHAR(3) NOT NULL,
  type                     ENUM('DEBIT','CREDIT','REFUND') NOT NULL,
  status                   ENUM('PENDING','COMPLETED','FAILED','CANCELLED','REVERSED') NOT NULL,
  transaction_time         DATETIME(6) NOT NULL,
  received_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  evaluation_mode          ENUM('REAL_TIME','LATE_ARRIVAL') NULL,
  evaluated_at             DATETIME(6) NULL,
  description              VARCHAR(500) NULL,
  ip_address               VARCHAR(45) NULL,
  device_id                VARCHAR(256) NULL,
  channel                  ENUM('WEB','APP','API','BRANCH') NULL,
  country_code             CHAR(2) NULL,
  payment_method           ENUM('BANK_TRANSFER','CARD','WALLET') NULL,
  reference_transaction_id VARCHAR(64) NULL,
  payload_hash             CHAR(64) NOT NULL,
  version                  INT UNSIGNED NOT NULL DEFAULT 0,
  created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT fk_transaction_payee FOREIGN KEY (payee_id) REFERENCES payees(id),
  CONSTRAINT fk_transaction_reference FOREIGN KEY (reference_transaction_id) REFERENCES transactions(id),
  CONSTRAINT chk_transaction_amount CHECK (amount > 0),
  CONSTRAINT chk_transaction_currency CHECK (currency = UPPER(currency)),
  CONSTRAINT chk_transaction_country CHECK (country_code IS NULL OR country_code = UPPER(country_code)),
  CONSTRAINT chk_transaction_payload_hash CHECK (payload_hash REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT chk_refund_reference CHECK (type <> 'REFUND' OR reference_transaction_id IS NOT NULL),
  INDEX idx_transactions_account_time (account_id, status, transaction_time, received_at, id),
  INDEX idx_transactions_account_payee_time (account_id, payee_id, status, transaction_time, received_at, id),
  INDEX idx_transactions_status_time (status, transaction_time DESC, id DESC),
  INDEX idx_transactions_payee (payee_id, transaction_time DESC, id DESC),
  INDEX idx_transactions_reference (reference_transaction_id)
) ENGINE=InnoDB;

CREATE TABLE alerts (
  id                           VARCHAR(64) PRIMARY KEY,
  rule_id                      VARCHAR(64) NOT NULL,
  rule_version                 INT UNSIGNED NOT NULL,
  account_id                   VARCHAR(64) NOT NULL,
  primary_payee_id             VARCHAR(64) NULL,
  first_trigger_transaction_id VARCHAR(64) NOT NULL,
  deduplication_key            VARCHAR(256) NOT NULL,
  status                       ENUM('OPEN','ACKNOWLEDGED','INVESTIGATING','CLOSED','DISMISSED') NOT NULL DEFAULT 'OPEN',
  severity                     ENUM('LOW','MEDIUM','HIGH') NOT NULL,
  reason_text                  VARCHAR(2000) NOT NULL,
  rule_snapshot                JSON NOT NULL,
  trigger_context              JSON NOT NULL,
  schema_version               INT UNSIGNED NOT NULL DEFAULT 1,
  episode_start                DATETIME(6) NOT NULL,
  last_triggered_at            DATETIME(6) NOT NULL,
  transaction_count            INT UNSIGNED NOT NULL DEFAULT 1,
  cumulative_amount            DECIMAL(19,4) NULL,
  resolution_code              ENUM('LEGITIMATE_ACTIVITY','FALSE_POSITIVE','DUPLICATE_ALERT','RULE_CONFIGURATION_ISSUE','SUSPICIOUS_ACTIVITY','OTHER') NULL,
  resolution_notes             VARCHAR(2000) NULL,
  version                      INT UNSIGNED NOT NULL DEFAULT 1,
  created_at                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  acknowledged_at              DATETIME(6) NULL,
  investigation_started_at     DATETIME(6) NULL,
  closed_at                    DATETIME(6) NULL,
  dismissed_at                 DATETIME(6) NULL,
  terminal_rank                TINYINT AS (CASE WHEN status IN ('OPEN','ACKNOWLEDGED','INVESTIGATING') THEN 0 ELSE 1 END) STORED,
  severity_rank                TINYINT AS (CASE severity WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END) STORED,
  CONSTRAINT fk_alert_rule FOREIGN KEY (rule_id) REFERENCES rules(id),
  CONSTRAINT fk_alert_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT fk_alert_payee FOREIGN KEY (primary_payee_id) REFERENCES payees(id),
  CONSTRAINT fk_alert_first_transaction FOREIGN KEY (first_trigger_transaction_id) REFERENCES transactions(id),
  CONSTRAINT uq_alert_deduplication_key UNIQUE (deduplication_key),
  CONSTRAINT chk_alert_rule_version CHECK (rule_version > 0),
  CONSTRAINT chk_alert_schema_version CHECK (schema_version > 0),
  CONSTRAINT chk_alert_transaction_count CHECK (transaction_count > 0),
  CONSTRAINT chk_alert_resolution CHECK (
    (status = 'CLOSED' AND resolution_code IS NOT NULL AND resolution_notes IS NOT NULL)
    OR (status = 'DISMISSED' AND resolution_notes IS NOT NULL)
    OR status IN ('OPEN','ACKNOWLEDGED','INVESTIGATING')
  ),
  CONSTRAINT chk_alert_terminal_timestamp CHECK (
    (status <> 'CLOSED' OR closed_at IS NOT NULL)
    AND (status <> 'DISMISSED' OR dismissed_at IS NOT NULL)
  ),
  INDEX idx_alerts_queue (terminal_rank, severity_rank, created_at DESC, id DESC),
  INDEX idx_alerts_episode_lookup (rule_id, account_id, status, last_triggered_at DESC),
  INDEX idx_alerts_account (account_id, created_at DESC, id DESC),
  INDEX idx_alerts_payee (primary_payee_id, created_at DESC, id DESC),
  INDEX idx_alerts_rule (rule_id, created_at DESC, id DESC)
) ENGINE=InnoDB;

CREATE TABLE alert_transactions (
  alert_id                 VARCHAR(64) NOT NULL,
  transaction_id           VARCHAR(64) NOT NULL,
  linked_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  link_reason              ENUM('FIRST_TRIGGER','WINDOW_CONTEXT','SUBSEQUENT_TRIGGER') NOT NULL,
  PRIMARY KEY (alert_id, transaction_id),
  CONSTRAINT fk_alert_transaction_alert FOREIGN KEY (alert_id) REFERENCES alerts(id),
  CONSTRAINT fk_alert_transaction_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
  INDEX idx_alert_transactions_transaction (transaction_id, alert_id)
) ENGINE=InnoDB;

CREATE TABLE alert_history (
  id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  alert_id                 VARCHAR(64) NOT NULL,
  event_type               ENUM('CREATED','TRANSACTION_LINKED','TRIGGER_METRICS_UPDATED','STATUS_CHANGED','RESOLUTION_UPDATED') NOT NULL,
  from_status              ENUM('OPEN','ACKNOWLEDGED','INVESTIGATING','CLOSED','DISMISSED') NULL,
  to_status                ENUM('OPEN','ACKNOWLEDGED','INVESTIGATING','CLOSED','DISMISSED') NULL,
  note                     VARCHAR(2000) NULL,
  previous_values          JSON NULL,
  new_values               JSON NULL,
  changed_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  changed_by               VARCHAR(128) NOT NULL DEFAULT 'SYSTEM',
  source                   ENUM('SYSTEM','OPERATOR','BATCH') NOT NULL DEFAULT 'SYSTEM',
  correlation_id           VARCHAR(128) NOT NULL,
  CONSTRAINT fk_alert_history_alert FOREIGN KEY (alert_id) REFERENCES alerts(id),
  CONSTRAINT chk_alert_history_status_event CHECK (
    event_type <> 'STATUS_CHANGED' OR (from_status IS NOT NULL AND to_status IS NOT NULL)
  ),
  INDEX idx_alert_history_timeline (alert_id, changed_at, id)
) ENGINE=InnoDB;

CREATE TABLE idempotency_records (
  id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  operation_scope          VARCHAR(200) NOT NULL,
  idempotency_key          VARCHAR(128) NOT NULL,
  request_hash             CHAR(64) NOT NULL,
  status                   ENUM('PROCESSING','SUCCEEDED','FAILED') NOT NULL,
  http_status              SMALLINT UNSIGNED NULL,
  response_body            JSON NULL,
  resource_id              VARCHAR(128) NULL,
  created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expires_at               DATETIME(6) NOT NULL,
  CONSTRAINT uq_idempotency_scope_key UNIQUE (operation_scope, idempotency_key),
  CONSTRAINT chk_idempotency_hash CHECK (request_hash REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT chk_idempotency_expiry CHECK (expires_at > created_at),
  INDEX idx_idempotency_expiry (expires_at)
) ENGINE=InnoDB;

DELIMITER $$
CREATE TRIGGER trg_alert_history_no_update
BEFORE UPDATE ON alert_history FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert_history is append-only';
END$$
CREATE TRIGGER trg_alert_history_no_delete
BEFORE DELETE ON alert_history FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'alert_history is append-only';
END$$
CREATE TRIGGER trg_rule_history_no_update
BEFORE UPDATE ON rule_history FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rule_history is append-only';
END$$
CREATE TRIGGER trg_rule_history_no_delete
BEFORE DELETE ON rule_history FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'rule_history is append-only';
END$$
DELIMITER ;

-- Application roles should receive SELECT/INSERT on history tables, but no UPDATE/DELETE.
-- For concurrent Velocity/Daily evaluation, the service locks accounts by:
-- SELECT id FROM accounts WHERE id = ? FOR UPDATE;
