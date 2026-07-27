const express = require("express");
const cors = require("cors");
const path = require("path");
const { getDb } = require("./db");
const { evaluateTransaction } = require("./framlEngine");
const { ALLOWED_TRANSITIONS, ALERT_STATUS } = require("./constants");

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "..", "public")));

function parsePagination(query) {
  const limit = Math.min(200, Math.max(1, Number(query.limit) || 50));
  const offset = Math.max(0, Number(query.offset) || 0);
  return { limit, offset };
}

app.get("/api/health", (_req, res) => {
  res.json({ status: "ok", model: "FRAML_BASELINE_V1" });
});

app.post("/api/transactions", async (req, res) => {
  const db = await getDb();
  const {
    account_id,
    payee_id,
    amount,
    currency = "USD",
    direction = "DEBIT",
    occurred_at,
    metadata,
  } = req.body;

  if (!account_id || !payee_id || !Number.isFinite(amount) || amount <= 0) {
    return res.status(400).json({ error: "account_id, payee_id and positive amount are required." });
  }

  const eventTime = occurred_at || new Date().toISOString();
  const txInsert = await db.run(
    `INSERT INTO transactions (account_id, payee_id, amount, currency, direction, occurred_at, metadata_json)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [account_id, payee_id, amount, currency, direction, eventTime, JSON.stringify(metadata || {})]
  );

  const tx = await db.get("SELECT * FROM transactions WHERE id = ?", [txInsert.lastID]);
  const alertOutcomes = await evaluateTransaction(db, tx);

  return res.status(201).json({
    transaction: tx,
    alerts: alertOutcomes,
  });
});

app.get("/api/transactions", async (req, res) => {
  const db = await getDb();
  const { limit, offset } = parsePagination(req.query);

  const filters = [];
  const values = [];

  if (req.query.account_id) {
    filters.push("account_id = ?");
    values.push(req.query.account_id);
  }
  if (req.query.q) {
    filters.push("(account_id LIKE ? OR payee_id LIKE ?)");
    values.push(`%${req.query.q}%`, `%${req.query.q}%`);
  }

  const where = filters.length ? `WHERE ${filters.join(" AND ")}` : "";
  const rows = await db.all(
    `SELECT * FROM transactions ${where}
     ORDER BY occurred_at DESC LIMIT ? OFFSET ?`,
    [...values, limit, offset]
  );

  res.json({ items: rows, limit, offset });
});

app.get("/api/alerts", async (req, res) => {
  const db = await getDb();
  const { limit, offset } = parsePagination(req.query);
  const filters = [];
  const values = [];

  if (req.query.status) {
    filters.push("a.status = ?");
    values.push(req.query.status);
  }
  if (req.query.severity) {
    filters.push("a.severity = ?");
    values.push(req.query.severity);
  }

  const where = filters.length ? `WHERE ${filters.join(" AND ")}` : "";

  const rows = await db.all(
    `SELECT a.*, r.name as rule_name, r.type as rule_type
     FROM alerts a
     JOIN rules r ON r.id = a.rule_id
     ${where}
     ORDER BY a.created_at DESC LIMIT ? OFFSET ?`,
    [...values, limit, offset]
  );

  res.json({ items: rows, limit, offset });
});

app.get("/api/alerts/history", async (req, res) => {
  const db = await getDb();
  const rows = await db.all(
    `SELECT a.*, r.name as rule_name
     FROM alerts a
     JOIN rules r ON r.id = a.rule_id
     WHERE a.status IN ('CLOSED', 'DISMISSED')
     ORDER BY a.updated_at DESC
     LIMIT 200`
  );
  res.json({ items: rows });
});

app.get("/api/alerts/:id", async (req, res) => {
  const db = await getDb();
  const id = Number(req.params.id);
  const alert = await db.get(
    `SELECT a.*, r.name as rule_name, r.type as rule_type
     FROM alerts a
     JOIN rules r ON r.id = a.rule_id
     WHERE a.id = ?`,
    [id]
  );

  if (!alert) {
    return res.status(404).json({ error: "Alert not found" });
  }

  const transaction = await db.get(
    "SELECT * FROM transactions WHERE id = ?",
    [alert.transaction_id]
  );

  const events = await db.all(
    "SELECT * FROM alert_events WHERE alert_id = ? ORDER BY created_at ASC",
    [id]
  );

  res.json({ alert, transaction, events });
});

app.patch("/api/alerts/:id/status", async (req, res) => {
  const db = await getDb();
  const id = Number(req.params.id);
  const { status, note, reason } = req.body;

  if (!status) {
    return res.status(400).json({ error: "New status is required." });
  }

  const alert = await db.get("SELECT * FROM alerts WHERE id = ?", [id]);
  if (!alert) {
    return res.status(404).json({ error: "Alert not found" });
  }

  const allowed = ALLOWED_TRANSITIONS[alert.status] || [];
  if (!allowed.includes(status)) {
    return res.status(400).json({
      error: `Invalid status transition from ${alert.status} to ${status}`,
      allowed,
    });
  }

  await db.run(
    `UPDATE alerts
     SET status = ?,
         updated_at = datetime('now'),
         resolution_reason = COALESCE(?, resolution_reason),
         notes = COALESCE(notes, '') || ?
     WHERE id = ?`,
    [status, reason || null, note ? `\n${note}` : "", id]
  );

  await db.run(
    `INSERT INTO alert_events (alert_id, from_status, to_status, note)
     VALUES (?, ?, ?, ?)`,
    [id, alert.status, status, note || "Manual lifecycle update"]
  );

  const updated = await db.get("SELECT * FROM alerts WHERE id = ?", [id]);
  res.json({ alert: updated });
});

app.get("/api/rules", async (_req, res) => {
  const db = await getDb();
  const rows = await db.all("SELECT * FROM rules ORDER BY id ASC");
  const items = rows.map((row) => ({
    ...row,
    config: JSON.parse(row.config_json),
  }));
  res.json({ items });
});

app.post("/api/rules", async (req, res) => {
  const db = await getDb();
  const { name, type, severity, config } = req.body;

  if (!name || !type || !severity || !config) {
    return res.status(400).json({ error: "name, type, severity, config are required" });
  }

  const insert = await db.run(
    `INSERT INTO rules (name, type, severity, config_json)
     VALUES (?, ?, ?, ?)`,
    [name, type, severity, JSON.stringify(config)]
  );

  await db.run(
    `INSERT INTO rule_change_log (rule_id, action, new_config_json)
     VALUES (?, 'CREATE', ?)`,
    [insert.lastID, JSON.stringify(config)]
  );

  const created = await db.get("SELECT * FROM rules WHERE id = ?", [insert.lastID]);
  res.status(201).json({ item: created });
});

app.put("/api/rules/:id", async (req, res) => {
  const db = await getDb();
  const id = Number(req.params.id);
  const { name, severity, is_active, config } = req.body;

  const existing = await db.get("SELECT * FROM rules WHERE id = ?", [id]);
  if (!existing) {
    return res.status(404).json({ error: "Rule not found" });
  }

  const newConfig = config || JSON.parse(existing.config_json);
  await db.run(
    `UPDATE rules
     SET name = ?,
         severity = ?,
         is_active = ?,
         config_json = ?,
         version = version + 1,
         updated_at = datetime('now')
     WHERE id = ?`,
    [
      name || existing.name,
      severity || existing.severity,
      is_active === undefined ? existing.is_active : (is_active ? 1 : 0),
      JSON.stringify(newConfig),
      id,
    ]
  );

  await db.run(
    `INSERT INTO rule_change_log (rule_id, action, previous_config_json, new_config_json)
     VALUES (?, 'UPDATE', ?, ?)`,
    [id, existing.config_json, JSON.stringify(newConfig)]
  );

  const updated = await db.get("SELECT * FROM rules WHERE id = ?", [id]);
  res.json({ item: updated });
});

app.patch("/api/rules/:id/toggle", async (req, res) => {
  const db = await getDb();
  const id = Number(req.params.id);
  const rule = await db.get("SELECT * FROM rules WHERE id = ?", [id]);
  if (!rule) {
    return res.status(404).json({ error: "Rule not found" });
  }

  const next = rule.is_active ? 0 : 1;
  await db.run(
    `UPDATE rules SET is_active = ?, version = version + 1, updated_at = datetime('now') WHERE id = ?`,
    [next, id]
  );

  await db.run(
    `INSERT INTO rule_change_log (rule_id, action, previous_config_json, new_config_json)
     VALUES (?, ?, ?, ?)`,
    [id, next ? "ACTIVATE" : "DEACTIVATE", rule.config_json, rule.config_json]
  );

  const updated = await db.get("SELECT * FROM rules WHERE id = ?", [id]);
  res.json({ item: updated });
});

app.get("/api/rules/performance", async (_req, res) => {
  const db = await getDb();
  const rows = await db.all(
    `SELECT r.id,
            r.name,
            r.type,
            COUNT(a.id) as total_alerts,
            SUM(CASE WHEN a.status IN ('CLOSED', 'DISMISSED') THEN 1 ELSE 0 END) as resolved_alerts,
            SUM(CASE WHEN a.status = 'DISMISSED' THEN 1 ELSE 0 END) as dismissed_alerts,
            AVG(a.score) as avg_score
     FROM rules r
     LEFT JOIN alerts a ON a.rule_id = r.id
     GROUP BY r.id, r.name, r.type
     ORDER BY total_alerts DESC, r.id ASC`
  );

  const items = rows.map((r) => {
    const total = r.total_alerts || 0;
    const dismissedRate = total === 0 ? 0 : Number((r.dismissed_alerts / total).toFixed(2));
    const conversionRate = total === 0 ? 0 : Number((r.resolved_alerts / total).toFixed(2));
    return {
      ...r,
      avg_score: r.avg_score ? Number(r.avg_score.toFixed(2)) : 0,
      dismissed_rate: dismissedRate,
      conversion_rate: conversionRate,
    };
  });

  res.json({ items });
});

app.get("/api/dashboard/summary", async (_req, res) => {
  const db = await getDb();

  const [statusRows, todayRow, avgResolutionRow] = await Promise.all([
    db.all("SELECT status, COUNT(*) as count FROM alerts GROUP BY status"),
    db.get(
      `SELECT COUNT(*) as alerts_today
       FROM alerts
       WHERE date(created_at) = date('now')`
    ),
    db.get(
      `SELECT AVG((julianday(updated_at) - julianday(created_at)) * 24 * 60) as avg_minutes
       FROM alerts
       WHERE status IN ('CLOSED', 'DISMISSED')`
    ),
  ]);

  const statusMap = {
    [ALERT_STATUS.OPEN]: 0,
    [ALERT_STATUS.ACKNOWLEDGED]: 0,
    [ALERT_STATUS.INVESTIGATING]: 0,
    [ALERT_STATUS.CLOSED]: 0,
    [ALERT_STATUS.DISMISSED]: 0,
  };

  for (const row of statusRows) {
    statusMap[row.status] = row.count;
  }

  res.json({
    status_counts: statusMap,
    alerts_today: todayRow.alerts_today,
    avg_resolution_minutes: avgResolutionRow.avg_minutes
      ? Number(avgResolutionRow.avg_minutes.toFixed(2))
      : 0,
  });
});

app.post("/api/demo/reset", async (_req, res) => {
  const db = await getDb();
  const pattern = "DEMO-%";

  const deleteEvents = await db.run(
    `DELETE FROM alert_events
     WHERE alert_id IN (
       SELECT id FROM alerts WHERE account_id LIKE ?
     )`,
    [pattern]
  );

  const deleteAlerts = await db.run(
    "DELETE FROM alerts WHERE account_id LIKE ?",
    [pattern]
  );

  const deleteTransactions = await db.run(
    "DELETE FROM transactions WHERE account_id LIKE ?",
    [pattern]
  );

  const deleteProfiles = await db.run(
    "DELETE FROM customer_risk_profiles WHERE account_id LIKE ?",
    [pattern]
  );

  res.json({
    deleted: {
      alert_events: deleteEvents.changes || 0,
      alerts: deleteAlerts.changes || 0,
      transactions: deleteTransactions.changes || 0,
      risk_profiles: deleteProfiles.changes || 0,
    },
  });
});

app.use((error, _req, res, _next) => {
  console.error(error);
  res.status(500).json({ error: "Internal server error", detail: error.message });
});

module.exports = app;
