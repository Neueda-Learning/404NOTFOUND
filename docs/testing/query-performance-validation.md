# Query Performance Validation

This document provides a repeatable way to validate database query performance after schema/query changes.

## Scope

The following hot paths were optimized:

1. Alert list/search filtering by status/severity/account/time.
2. Transaction list/search filtering by account/payee/status/type/time.
3. Rule-engine aggregation queries (velocity and daily-limit checks).
4. Transaction-to-alert lookup used in transaction detail APIs.

## Indexes Added

1. `alerts`
   - `idx_alert_status_created (status, created_at)`
   - `idx_alert_severity_created (severity, created_at)`
   - `idx_alert_account_created (account_id, created_at)`
   - `idx_alert_primary_tx (primary_transaction_id)`
2. `transactions`
   - `idx_tx_account_status_type_time (account_id, status, type, transaction_time)`
   - `idx_tx_account_currency_status_type_time (account_id, currency, status, type, transaction_time)`
   - `idx_tx_account_payee_status_time (account_id, payee_id, status, transaction_time)`
3. `alert_transactions`
   - `idx_alert_tx_transaction_id (transaction_id)`
   - `idx_alert_tx_alert_id (alert_id)`
   - `idx_alert_tx_transaction_alert (transaction_id, alert_id)`

## Validation Steps (MySQL 8)

Run these checks against a representative dataset.

```sql
-- 1) Alerts search path
EXPLAIN ANALYZE
SELECT *
FROM alerts a
WHERE a.status = 'OPEN'
  AND a.created_at >= NOW() - INTERVAL 7 DAY
ORDER BY a.created_at DESC
LIMIT 20;

-- 2) Transactions search path
EXPLAIN ANALYZE
SELECT *
FROM transactions t
WHERE t.account_id = 'ACC-001'
  AND t.status = 'COMPLETED'
  AND t.type = 'DEBIT'
  AND t.transaction_time >= NOW() - INTERVAL 1 DAY
ORDER BY t.transaction_time DESC
LIMIT 20;

-- 3) Velocity aggregation path
EXPLAIN ANALYZE
SELECT COUNT(*)
FROM transactions t
WHERE t.account_id = 'ACC-001'
  AND t.status = 'COMPLETED'
  AND t.type IN ('DEBIT','TRANSFER')
  AND t.transaction_time BETWEEN NOW() - INTERVAL 10 MINUTE AND NOW();

-- 4) Daily-limit aggregation path
EXPLAIN ANALYZE
SELECT SUM(t.amount)
FROM transactions t
WHERE t.account_id = 'ACC-001'
  AND t.currency = 'USD'
  AND t.status = 'COMPLETED'
  AND t.type = 'DEBIT'
  AND t.transaction_time BETWEEN DATE(NOW()) AND NOW();

-- 5) Transaction-alert relationship path
EXPLAIN ANALYZE
SELECT a.*
FROM alerts a
WHERE a.primary_transaction_id = 'TXN-001'
   OR EXISTS (
      SELECT 1
      FROM alert_transactions at
      WHERE at.alert_id = a.alert_id
        AND at.transaction_id = 'TXN-001'
   );
```

## Expected Signals

1. `EXPLAIN ANALYZE` should show index access on the new composite indexes.
2. Full table scans should be avoided for the filtered queries above.
3. `rows examined` should scale with filter selectivity, not total table size.
