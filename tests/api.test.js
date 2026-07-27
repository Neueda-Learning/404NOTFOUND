process.env.DATABASE_FILE = ":memory:";

const request = require("supertest");
const app = require("../src/app");
const { getDb, resetDbForTests } = require("../src/db");

async function createTransaction(payload = {}) {
  return request(app)
    .post("/api/transactions")
    .send({
      account_id: "ACC-001",
      payee_id: "PAYEE-001",
      amount: 12000,
      direction: "DEBIT",
      ...payload,
    });
}

describe("FRAML Transaction Monitoring API", () => {
  beforeEach(async () => {
    await resetDbForTests();
    await getDb();
  });

  afterAll(async () => {
    await resetDbForTests();
  });

  test("health endpoint returns FRAML model marker", async () => {
    const response = await request(app).get("/api/health");
    expect(response.status).toBe(200);
    expect(response.body.model).toBe("FRAML_BASELINE_V1");
  });

  test("creating high-value transaction triggers rules and alerts", async () => {
    const response = await createTransaction();
    expect(response.status).toBe(201);
    expect(response.body.alerts.length).toBeGreaterThanOrEqual(1);

    const alertsResponse = await request(app).get("/api/alerts");
    expect(alertsResponse.status).toBe(200);
    expect(alertsResponse.body.items.length).toBeGreaterThanOrEqual(1);
  });

  test("deduplication avoids duplicate active alerts", async () => {
    await createTransaction();
    const second = await createTransaction({ amount: 13000, occurred_at: new Date().toISOString() });

    const amountRuleOutcome = second.body.alerts.find((a) => a.ruleType === "AMOUNT_THRESHOLD");
    expect(amountRuleOutcome).toBeDefined();
    expect(amountRuleOutcome.created).toBe(false);
  });

  test("new payee rule only triggers on first seen payee", async () => {
    const first = await createTransaction({ payee_id: "PAYEE-NEW" });
    const firstHit = first.body.alerts.find((a) => a.ruleType === "NEW_PAYEE");
    expect(firstHit).toBeDefined();

    const second = await createTransaction({ payee_id: "PAYEE-NEW", amount: 500 });
    const secondHit = second.body.alerts.find((a) => a.ruleType === "NEW_PAYEE");
    expect(secondHit).toBeDefined();
    expect(secondHit.created).toBe(false);
  });

  test("rejects invalid transaction payload", async () => {
    const response = await request(app).post("/api/transactions").send({
      account_id: "ACC-BAD",
      amount: -1,
    });
    expect(response.status).toBe(400);
  });

  test("returns 404 for missing alert and supports rule create/update", async () => {
    const missing = await request(app).get("/api/alerts/9999");
    expect(missing.status).toBe(404);

    const created = await request(app).post("/api/rules").send({
      name: "Temporary Rule",
      type: "AMOUNT_THRESHOLD",
      severity: "LOW",
      config: { threshold: 99999, dedupWindowMinutes: 30 },
    });
    expect(created.status).toBe(201);

    const updated = await request(app)
      .put(`/api/rules/${created.body.item.id}`)
      .send({
        name: "Temporary Rule V2",
        severity: "MEDIUM",
        is_active: true,
        config: { threshold: 88888, dedupWindowMinutes: 15 },
      });
    expect(updated.status).toBe(200);
    expect(updated.body.item.version).toBeGreaterThan(1);
  });

  test("alert lifecycle allows valid transition sequence", async () => {
    await createTransaction();
    const alerts = await request(app).get("/api/alerts?status=OPEN");
    const alertId = alerts.body.items[0].id;

    const ack = await request(app)
      .patch(`/api/alerts/${alertId}/status`)
      .send({ status: "ACKNOWLEDGED", note: "Seen" });
    expect(ack.status).toBe(200);

    const investigating = await request(app)
      .patch(`/api/alerts/${alertId}/status`)
      .send({ status: "INVESTIGATING", note: "Started investigation" });
    expect(investigating.status).toBe(200);

    const closed = await request(app)
      .patch(`/api/alerts/${alertId}/status`)
      .send({ status: "CLOSED", reason: "Legitimate", note: "Completed" });
    expect(closed.status).toBe(200);

    const invalid = await request(app)
      .patch(`/api/alerts/${alertId}/status`)
      .send({ status: "OPEN" });
    expect(invalid.status).toBe(400);
  });

  test("rules can be toggled and performance endpoint is available", async () => {
    const rulesBefore = await request(app).get("/api/rules");
    const amountRule = rulesBefore.body.items.find((r) => r.type === "AMOUNT_THRESHOLD");

    const toggled = await request(app).patch(`/api/rules/${amountRule.id}/toggle`);
    expect(toggled.status).toBe(200);

    const tx = await createTransaction({ account_id: "ACC-TOGGLE", payee_id: "PAYEE-Z", amount: 22000 });
    const amountOutcome = tx.body.alerts.find((a) => a.ruleType === "AMOUNT_THRESHOLD");
    expect(amountOutcome).toBeUndefined();

    const perf = await request(app).get("/api/rules/performance");
    expect(perf.status).toBe(200);
    expect(perf.body.items.length).toBeGreaterThan(0);
  });

  test("history endpoint returns resolved alerts", async () => {
    await createTransaction({ account_id: "ACC-HISTORY", payee_id: "PAYEE-H1", amount: 14000 });
    const alerts = await request(app).get("/api/alerts?status=OPEN");
    const alertId = alerts.body.items[0].id;

    await request(app)
      .patch(`/api/alerts/${alertId}/status`)
      .send({ status: "ACKNOWLEDGED", note: "Ack" });
    await request(app)
      .patch(`/api/alerts/${alertId}/status`)
      .send({ status: "INVESTIGATING", note: "Investigating" });
    await request(app)
      .patch(`/api/alerts/${alertId}/status`)
      .send({ status: "DISMISSED", reason: "False positive" });

    const history = await request(app).get("/api/alerts/history");
    expect(history.status).toBe(200);
    expect(history.body.items.length).toBeGreaterThan(0);
  });
});
