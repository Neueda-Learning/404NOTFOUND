const { ALERT_STATUS, RULE_TYPES } = require("./constants");

const riskBaseScore = {
  LOW: 20,
  MEDIUM: 50,
  HIGH: 80,
};

const severityWeight = {
  LOW: 10,
  MEDIUM: 20,
  HIGH: 30,
};

function parseConfig(rule) {
  return JSON.parse(rule.config_json || "{}");
}

function getRiskBandThreshold(config, key, riskBand, fallback) {
  const mapName = `${key}ByRiskBand`;
  const map = config[mapName] || {};
  return map[riskBand] ?? config[key] ?? fallback;
}

function scoreAlert(ruleSeverity, riskBand, multiplier = 1) {
  const risk = riskBaseScore[riskBand] ?? 50;
  const sev = severityWeight[ruleSeverity] ?? 10;
  return Math.min(100, Math.round((risk + sev) * multiplier));
}

function dedupKey(ruleId, accountId, occurredAt, windowMinutes) {
  const date = new Date(occurredAt);
  const bucket = Math.floor(date.getTime() / (windowMinutes * 60 * 1000));
  return `${ruleId}:${accountId}:${bucket}`;
}

async function ensureRiskProfile(db, accountId) {
  const profile = await db.get(
    "SELECT account_id, risk_band FROM customer_risk_profiles WHERE account_id = ?",
    [accountId]
  );
  if (profile) {
    return profile.risk_band;
  }

  await db.run(
    "INSERT INTO customer_risk_profiles (account_id, risk_band) VALUES (?, 'MEDIUM')",
    [accountId]
  );
  return "MEDIUM";
}

async function evaluateRule(db, rule, transaction, riskBand) {
  const config = parseConfig(rule);
  const dedupWindow = config.dedupWindowMinutes || 60;

  if (rule.type === RULE_TYPES.AMOUNT_THRESHOLD) {
    const threshold = getRiskBandThreshold(config, "threshold", riskBand, 10000);
    if (transaction.amount > threshold) {
      return {
        title: "Amount threshold exceeded",
        description: `Transaction amount ${transaction.amount} exceeded threshold ${threshold}.`,
        score: scoreAlert(rule.severity, riskBand, 1.1),
        dedupWindow,
      };
    }
  }

  if (rule.type === RULE_TYPES.VELOCITY) {
    const windowMinutes = config.windowMinutes || 10;
    const maxCount = getRiskBandThreshold(config, "maxCount", riskBand, 5);
    const countRow = await db.get(
      `SELECT COUNT(*) as tx_count
       FROM transactions
       WHERE account_id = ?
         AND occurred_at >= datetime(?, '-' || ? || ' minutes')
         AND occurred_at <= datetime(?)`,
      [transaction.account_id, transaction.occurred_at, windowMinutes, transaction.occurred_at]
    );

    if (countRow.tx_count > maxCount) {
      return {
        title: "Transaction velocity spike",
        description: `Detected ${countRow.tx_count} transactions in ${windowMinutes} minutes (limit ${maxCount}).`,
        score: scoreAlert(rule.severity, riskBand, 1.0 + countRow.tx_count / (maxCount + 1)),
        dedupWindow,
      };
    }
  }

  if (rule.type === RULE_TYPES.NEW_PAYEE) {
    const row = await db.get(
      `SELECT COUNT(*) as previous_count
       FROM transactions
       WHERE account_id = ?
         AND payee_id = ?
         AND id <> ?
         AND occurred_at < datetime(?)`,
      [transaction.account_id, transaction.payee_id, transaction.id, transaction.occurred_at]
    );

    if (row.previous_count === 0) {
      return {
        title: "First-time payee transfer",
        description: `Payee ${transaction.payee_id} has not been seen before for account ${transaction.account_id}.`,
        score: scoreAlert(rule.severity, riskBand, 0.95),
        dedupWindow,
      };
    }
  }

  if (rule.type === RULE_TYPES.DAILY_LIMIT) {
    const dailyLimit = getRiskBandThreshold(config, "dailyLimit", riskBand, 50000);
    const sumRow = await db.get(
      `SELECT COALESCE(SUM(amount), 0) as daily_total
       FROM transactions
       WHERE account_id = ?
         AND direction = 'DEBIT'
         AND date(occurred_at) = date(?)`,
      [transaction.account_id, transaction.occurred_at]
    );

    if (sumRow.daily_total > dailyLimit) {
      return {
        title: "Daily outgoing limit exceeded",
        description: `Outgoing total ${sumRow.daily_total} exceeded daily limit ${dailyLimit}.`,
        score: scoreAlert(rule.severity, riskBand, 1.15),
        dedupWindow,
      };
    }
  }

  return null;
}

async function upsertAlert(db, rule, transaction, hit, riskBand) {
  const dedup = dedupKey(rule.id, transaction.account_id, transaction.occurred_at, hit.dedupWindow);
  const groupKey = `${rule.type}:${transaction.account_id}:${transaction.occurred_at.slice(0, 10)}`;

  const existing = await db.get(
    `SELECT * FROM alerts
     WHERE dedup_key = ? AND status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING')`,
    [dedup]
  );

  if (existing) {
    await db.run(
      `UPDATE alerts
       SET updated_at = datetime('now'),
           notes = COALESCE(notes, '') || ?
       WHERE id = ?`,
      [`\nDuplicate hit for transaction ${transaction.id} at ${new Date().toISOString()}`, existing.id]
    );
    return { created: false, alertId: existing.id };
  }

  const insert = await db.run(
    `INSERT INTO alerts (
      rule_id, transaction_id, account_id, status, severity, score, title,
      description, dedup_key, group_key
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      rule.id,
      transaction.id,
      transaction.account_id,
      ALERT_STATUS.OPEN,
      rule.severity,
      hit.score,
      hit.title,
      `${hit.description} (riskBand=${riskBand})`,
      dedup,
      groupKey,
    ]
  );

  await db.run(
    `INSERT INTO alert_events (alert_id, from_status, to_status, note)
     VALUES (?, NULL, ?, ?)`,
    [insert.lastID, ALERT_STATUS.OPEN, "Alert created by FRAML engine"]
  );

  return { created: true, alertId: insert.lastID };
}

async function evaluateTransaction(db, transaction) {
  const riskBand = await ensureRiskProfile(db, transaction.account_id);
  const rules = await db.all(
    "SELECT * FROM rules WHERE is_active = 1 ORDER BY id ASC"
  );

  const outcomes = [];
  for (const rule of rules) {
    const hit = await evaluateRule(db, rule, transaction, riskBand);
    if (!hit) {
      continue;
    }
    const outcome = await upsertAlert(db, rule, transaction, hit, riskBand);
    outcomes.push({
      ruleId: rule.id,
      ruleType: rule.type,
      created: outcome.created,
      alertId: outcome.alertId,
    });
  }

  return outcomes;
}

module.exports = {
  evaluateTransaction,
};
