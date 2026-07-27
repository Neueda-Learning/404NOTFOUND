const fs = require("fs");
const path = require("path");
const sqlite3 = require("sqlite3");
const { open } = require("sqlite");
const { RULE_TYPES, SEVERITY } = require("./constants");

let dbPromise;

const defaultRules = [
  {
    name: "High Amount Transaction",
    type: RULE_TYPES.AMOUNT_THRESHOLD,
    severity: SEVERITY.HIGH,
    config: {
      threshold: 10000,
      thresholdByRiskBand: {
        LOW: 15000,
        MEDIUM: 10000,
        HIGH: 7000,
      },
      dedupWindowMinutes: 60,
    },
  },
  {
    name: "Velocity Spike",
    type: RULE_TYPES.VELOCITY,
    severity: SEVERITY.MEDIUM,
    config: {
      windowMinutes: 10,
      maxCount: 5,
      maxCountByRiskBand: {
        LOW: 7,
        MEDIUM: 5,
        HIGH: 3,
      },
      dedupWindowMinutes: 20,
    },
  },
  {
    name: "First-Time Payee",
    type: RULE_TYPES.NEW_PAYEE,
    severity: SEVERITY.MEDIUM,
    config: {
      dedupWindowMinutes: 1440,
    },
  },
  {
    name: "Daily Outgoing Limit",
    type: RULE_TYPES.DAILY_LIMIT,
    severity: SEVERITY.HIGH,
    config: {
      dailyLimit: 50000,
      dailyLimitByRiskBand: {
        LOW: 70000,
        MEDIUM: 50000,
        HIGH: 35000,
      },
      dedupWindowMinutes: 120,
    },
  },
];

async function initializeSchema(db) {
  await db.exec(`
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      account_id TEXT NOT NULL,
      payee_id TEXT NOT NULL,
      amount REAL NOT NULL,
      currency TEXT NOT NULL,
      direction TEXT NOT NULL,
      occurred_at TEXT NOT NULL,
      metadata_json TEXT,
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS customer_risk_profiles (
      account_id TEXT PRIMARY KEY,
      risk_band TEXT NOT NULL DEFAULT 'MEDIUM',
      updated_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS rules (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      type TEXT NOT NULL,
      severity TEXT NOT NULL,
      is_active INTEGER NOT NULL DEFAULT 1,
      version INTEGER NOT NULL DEFAULT 1,
      config_json TEXT NOT NULL,
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS rule_change_log (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      rule_id INTEGER NOT NULL,
      action TEXT NOT NULL,
      previous_config_json TEXT,
      new_config_json TEXT,
      changed_by TEXT NOT NULL DEFAULT 'operator',
      changed_at TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY(rule_id) REFERENCES rules(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS alerts (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      rule_id INTEGER NOT NULL,
      transaction_id INTEGER NOT NULL,
      account_id TEXT NOT NULL,
      status TEXT NOT NULL,
      severity TEXT NOT NULL,
      score INTEGER NOT NULL,
      title TEXT NOT NULL,
      description TEXT NOT NULL,
      dedup_key TEXT NOT NULL,
      group_key TEXT NOT NULL,
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at TEXT NOT NULL DEFAULT (datetime('now')),
      resolution_reason TEXT,
      notes TEXT,
      FOREIGN KEY(rule_id) REFERENCES rules(id),
      FOREIGN KEY(transaction_id) REFERENCES transactions(id)
    );

    CREATE TABLE IF NOT EXISTS alert_events (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      alert_id INTEGER NOT NULL,
      from_status TEXT,
      to_status TEXT NOT NULL,
      note TEXT,
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      FOREIGN KEY(alert_id) REFERENCES alerts(id) ON DELETE CASCADE
    );

    CREATE INDEX IF NOT EXISTS idx_transactions_account_time
      ON transactions(account_id, occurred_at);

    CREATE INDEX IF NOT EXISTS idx_transactions_payee_account
      ON transactions(account_id, payee_id);

    CREATE INDEX IF NOT EXISTS idx_alerts_status_created
      ON alerts(status, created_at);

    CREATE INDEX IF NOT EXISTS idx_alerts_dedup
      ON alerts(dedup_key, status);
  `);
}

async function seedDefaults(db) {
  const countRow = await db.get("SELECT COUNT(*) as count FROM rules");
  if (countRow.count > 0) {
    return;
  }

  for (const rule of defaultRules) {
    const result = await db.run(
      `INSERT INTO rules (name, type, severity, config_json)
       VALUES (?, ?, ?, ?)` ,
      [rule.name, rule.type, rule.severity, JSON.stringify(rule.config)]
    );

    await db.run(
      `INSERT INTO rule_change_log (rule_id, action, new_config_json)
       VALUES (?, 'CREATE', ?)`,
      [result.lastID, JSON.stringify(rule.config)]
    );
  }
}

function resolveDbFile() {
  const envPath = process.env.DATABASE_FILE;
  if (envPath) {
    return envPath;
  }
  const dataDir = path.join(__dirname, "..", "data");
  fs.mkdirSync(dataDir, { recursive: true });
  return path.join(dataDir, "app.db");
}

async function getDb() {
  if (!dbPromise) {
    dbPromise = open({
      filename: resolveDbFile(),
      driver: sqlite3.Database,
    }).then(async (db) => {
      await initializeSchema(db);
      await seedDefaults(db);
      return db;
    });
  }
  return dbPromise;
}

async function resetDbForTests() {
  if (!dbPromise) {
    return;
  }
  const db = await dbPromise;
  await db.close();
  dbPromise = undefined;
}

module.exports = {
  getDb,
  resetDbForTests,
};
