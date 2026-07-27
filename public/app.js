async function request(url, options = {}) {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || "Request failed");
  }
  return res.json();
}

const state = {
  selectedSeverity: "",
  selectedAlertId: null,
  alerts: [],
  rules: [],
  performance: [],
  summary: null,
  history: [],
};

let toastTimer;
let txSearchTimer;

function showToast(message, type = "success") {
  const toast = document.getElementById("toast");
  if (!toast) {
    return;
  }

  toast.className = `toast ${type}`;
  toast.textContent = message;
  toast.classList.add("show");

  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.classList.remove("show");
  }, 2800);
}

function emptyRow(columns, message) {
  return `<tr class="empty-row"><td colspan="${columns}">${message}</td></tr>`;
}

function formatNumber(value) {
  const numeric = Number(value || 0);
  return Number.isFinite(numeric) ? numeric.toLocaleString() : "0";
}

function formatPercent(ratio) {
  const numeric = Number(ratio || 0);
  return `${Math.round(numeric * 100)}%`;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString();
}

function formatMoney(value) {
  const numeric = Number(value || 0);
  if (!Number.isFinite(numeric)) {
    return "$0";
  }
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(numeric);
}

function statusBadge(status) {
  return `<span class="badge ${status}">${status}</span>`;
}

function statusAction(status) {
  if (status === "OPEN") return "ACKNOWLEDGED";
  if (status === "ACKNOWLEDGED") return "INVESTIGATING";
  if (status === "INVESTIGATING") return "CLOSED";
  return null;
}

function canDismiss(status) {
  return status === "OPEN" || status === "ACKNOWLEDGED" || status === "INVESTIGATING";
}

function setLastSync() {
  const target = document.getElementById("last-sync");
  if (!target) {
    return;
  }
  target.textContent = `Last sync: ${new Date().toLocaleTimeString()}`;
}

async function patchAlertStatus(alertId, status, note, reason) {
  return request(`/api/alerts/${alertId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status, note, reason }),
  });
}

function renderIntelligence() {
  const kpiHost = document.getElementById("intelligence-kpis");
  const actionsHost = document.getElementById("intelligence-actions");
  if (!kpiHost || !actionsHost || !state.summary) {
    return;
  }

  const open = Number(state.summary.status_counts?.OPEN || 0);
  const acknowledged = Number(state.summary.status_counts?.ACKNOWLEDGED || 0);
  const investigating = Number(state.summary.status_counts?.INVESTIGATING || 0);
  const highOpen = state.alerts.filter((a) => a.status === "OPEN" && a.severity === "HIGH").length;
  const avgResolve = Number(state.summary.avg_resolution_minutes || 0);

  let posture = "Stable";
  if (highOpen >= 2 || open >= 5) {
    posture = "Elevated";
  } else if (open > 0 || investigating > 0) {
    posture = "Guarded";
  }

  const topRule = [...state.performance].sort((a, b) => (b.total_alerts || 0) - (a.total_alerts || 0))[0];
  const topRuleName = topRule ? topRule.name : "No activity";
  const dismissedRate = topRule ? formatPercent(topRule.dismissed_rate) : "0%";

  kpiHost.innerHTML = `
    <div class="insight-item">
      <span>Risk Posture</span>
      <b>${posture}</b>
    </div>
    <div class="insight-item">
      <span>High OPEN Alerts</span>
      <b>${formatNumber(highOpen)}</b>
    </div>
    <div class="insight-item">
      <span>Primary Trigger</span>
      <b>${topRuleName}</b>
    </div>
    <div class="insight-item">
      <span>Avg Resolve</span>
      <b>${formatNumber(avgResolve)} min</b>
    </div>
  `;

  const recommendations = [];
  if (posture === "Elevated") {
    recommendations.push("Prioritize HIGH severity OPEN alerts and push them to INVESTIGATING.");
  }
  if (acknowledged > 3) {
    recommendations.push("ACKNOWLEDGED queue is growing. Consider bulk progression to INVESTIGATING.");
  }
  if (topRule && topRule.dismissed_rate >= 0.4) {
    recommendations.push(`Rule ${topRule.name} has high dismiss ratio (${dismissedRate}). Tune threshold or velocity window.`);
  }
  if (!recommendations.length) {
    recommendations.push("Current portfolio looks healthy. Keep monitoring conversion and false-positive trend.");
  }

  actionsHost.innerHTML = recommendations.map((item) => `<li>${item}</li>`).join("");
}

async function loadSummary() {
  const data = await request("/api/dashboard/summary");
  state.summary = data;
  const summary = document.getElementById("summary");
  summary.innerHTML = "";

  const blocks = [
    ["OPEN", data.status_counts.OPEN || 0, "critical"],
    ["ACKNOWLEDGED", data.status_counts.ACKNOWLEDGED || 0, "warning"],
    ["INVESTIGATING", data.status_counts.INVESTIGATING || 0, "warning"],
    ["CLOSED", data.status_counts.CLOSED || 0, "success"],
    ["DISMISSED", data.status_counts.DISMISSED || 0, "neutral"],
    ["Alerts Today", data.alerts_today, "neutral"],
    ["Avg Resolve (min)", data.avg_resolution_minutes, "success"],
  ];

  for (const [label, value, tone] of blocks) {
    const div = document.createElement("div");
    div.className = `summary-item ${tone}`;
    div.innerHTML = `<span>${label}</span><b>${formatNumber(value)}</b>`;
    summary.appendChild(div);
  }
}

async function loadAlertDetail(alertId) {
  const host = document.getElementById("alert-detail");
  if (!host) {
    return;
  }
  host.className = "detail-panel";
  host.textContent = "Loading alert details...";

  try {
    const data = await request(`/api/alerts/${alertId}`);
    const tx = data.transaction || {};
    const events = data.events || [];

    host.innerHTML = `
      <div class="detail-head">
        <div>
          <h3>Alert #${formatNumber(data.alert.id)}</h3>
          <p>${data.alert.rule_name} • ${data.alert.rule_type}</p>
        </div>
        ${statusBadge(data.alert.status)}
      </div>
      <p class="detail-description">${data.alert.description || "No description"}</p>
      <div class="detail-grid">
        <div><span>Severity</span><b>${data.alert.severity}</b></div>
        <div><span>Score</span><b>${formatNumber(data.alert.score)}</b></div>
        <div><span>Account</span><b>${data.alert.account_id}</b></div>
        <div><span>Created</span><b>${formatDateTime(data.alert.created_at)}</b></div>
      </div>
      <div class="detail-grid">
        <div><span>Linked Tx</span><b>#${formatNumber(tx.id)}</b></div>
        <div><span>Amount</span><b>${formatMoney(tx.amount)}</b></div>
        <div><span>Payee</span><b>${tx.payee_id || "-"}</b></div>
        <div><span>Occurred</span><b>${formatDateTime(tx.occurred_at)}</b></div>
      </div>
      <div class="timeline">
        <h4>Timeline</h4>
        ${events.length ? events.map((event) => `
          <div class="timeline-item">
            <span>${formatDateTime(event.created_at)}</span>
            <b>${event.from_status || "NEW"} → ${event.to_status || "-"}</b>
            <p>${event.note || "-"}</p>
          </div>
        `).join("") : "<p>No events.</p>"}
      </div>
    `;
  } catch (error) {
    host.className = "detail-panel empty";
    host.textContent = error.message;
  }
}

function renderAlertRows(items) {
  const body = document.getElementById("alerts-body");
  if (!items.length) {
    body.innerHTML = emptyRow(7, "No alerts for current filter.");
    return;
  }

  body.innerHTML = "";
  for (const alert of items) {
    const row = document.createElement("tr");
    if (state.selectedAlertId === alert.id) {
      row.classList.add("active-row");
    }

    const next = statusAction(alert.status);
    const scoreWidth = Math.max(6, Math.min(100, Number(alert.score || 0)));
    row.innerHTML = `
      <td data-label="ID">${formatNumber(alert.id)}</td>
      <td data-label="Rule">${alert.rule_name}</td>
      <td data-label="Status">${statusBadge(alert.status)}</td>
      <td data-label="Severity">${alert.severity}</td>
      <td data-label="Score">
        <div class="score-cell">
          <span>${formatNumber(alert.score)}</span>
          <div class="score-track"><div class="score-fill" style="width:${scoreWidth}%"></div></div>
        </div>
      </td>
      <td data-label="Created">${formatDateTime(alert.created_at)}</td>
      <td data-label="Action">
        <div class="action-pack">
          ${next ? `<button data-id="${alert.id}" data-next="${next}">${next}</button>` : ""}
          ${canDismiss(alert.status) ? `<button class="ghost" data-id="${alert.id}" data-dismiss="1">DISMISS</button>` : ""}
          <button class="secondary" data-view="${alert.id}">DETAILS</button>
        </div>
      </td>
    `;
    row.addEventListener("click", (event) => {
      if (event.target.closest("button")) {
        return;
      }
      state.selectedAlertId = alert.id;
      loadAlertDetail(alert.id);
      renderAlertRows(items);
    });
    body.appendChild(row);
  }

  body.querySelectorAll("button[data-next]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      try {
        await patchAlertStatus(btn.dataset.id, btn.dataset.next, "Lifecycle update from dashboard");
        showToast(`Alert #${btn.dataset.id} moved to ${btn.dataset.next}.`);
        await refreshAll();
      } catch (error) {
        showToast(error.message, "error");
      }
    });
  });

  body.querySelectorAll("button[data-dismiss]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const reason = window.prompt("Optional dismissal reason", "False positive") || "Manual dismissal";
      try {
        await patchAlertStatus(btn.dataset.id, "DISMISSED", "Dismissed from dashboard", reason);
        showToast(`Alert #${btn.dataset.id} dismissed.`);
        await refreshAll();
      } catch (error) {
        showToast(error.message, "error");
      }
    });
  });

  body.querySelectorAll("button[data-view]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.selectedAlertId = Number(btn.dataset.view);
      loadAlertDetail(btn.dataset.view);
      renderAlertRows(items);
    });
  });
}

async function loadAlerts() {
  const status = document.getElementById("alert-status-filter").value;
  const query = new URLSearchParams();
  if (status) {
    query.set("status", status);
  }
  if (state.selectedSeverity) {
    query.set("severity", state.selectedSeverity);
  }

  const body = document.getElementById("alerts-body");
  body.innerHTML = emptyRow(7, "Loading alerts...");
  const url = query.toString() ? `/api/alerts?${query.toString()}` : "/api/alerts";
  const data = await request(url);
  state.alerts = data.items || [];
  renderAlertRows(state.alerts);
}

async function loadTransactions() {
  const body = document.getElementById("transactions-body");
  body.innerHTML = emptyRow(6, "Loading transactions...");

  const query = new URLSearchParams();
  const q = document.getElementById("tx-search").value.trim();
  const account = document.getElementById("tx-account-filter").value.trim();
  if (q) {
    query.set("q", q);
  }
  if (account) {
    query.set("account_id", account);
  }
  query.set("limit", "100");

  const data = await request(`/api/transactions?${query.toString()}`);
  if (!data.items.length) {
    body.innerHTML = emptyRow(6, "No transactions found.");
    return;
  }

  body.innerHTML = "";
  for (const tx of data.items) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td data-label="ID">${formatNumber(tx.id)}</td>
      <td data-label="Account">${tx.account_id}</td>
      <td data-label="Payee">${tx.payee_id}</td>
      <td data-label="Amount">${formatMoney(tx.amount)}</td>
      <td data-label="Direction">${tx.direction}</td>
      <td data-label="Occurred At">${formatDateTime(tx.occurred_at)}</td>
    `;
    body.appendChild(row);
  }
}

function populateRuleForm(rule) {
  if (!rule) {
    return;
  }
  document.getElementById("rule-select").value = String(rule.id);
  document.getElementById("rule-name").value = rule.name || "";
  document.getElementById("rule-severity").value = rule.severity || "MEDIUM";

  const config = rule.config || {};
  document.getElementById("rule-threshold").value = config.threshold ?? "";
  document.getElementById("rule-max-count").value = config.maxCount ?? "";
  document.getElementById("rule-window-minutes").value = config.windowMinutes ?? "";
  document.getElementById("rule-daily-limit").value = config.dailyLimit ?? "";
  document.getElementById("rule-dedup-window").value = config.dedupWindowMinutes ?? "";
}

function readNumberInput(id) {
  const raw = document.getElementById(id).value;
  if (raw === "") {
    return undefined;
  }
  const numeric = Number(raw);
  return Number.isFinite(numeric) ? numeric : undefined;
}

async function loadRules() {
  const body = document.getElementById("rules-body");
  body.innerHTML = emptyRow(6, "Loading rules...");
  const data = await request("/api/rules");
  state.rules = data.items || [];

  const ruleSelect = document.getElementById("rule-select");
  ruleSelect.innerHTML = state.rules
    .map((rule) => `<option value="${rule.id}">${rule.id} • ${rule.name}</option>`)
    .join("");

  if (!state.rules.length) {
    body.innerHTML = emptyRow(6, "No rules found.");
    return;
  }

  const current = state.rules.find((rule) => String(rule.id) === ruleSelect.value) || state.rules[0];
  populateRuleForm(current);

  body.innerHTML = "";
  for (const rule of state.rules) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td data-label="ID">${formatNumber(rule.id)}</td>
      <td data-label="Name">${rule.name}</td>
      <td data-label="Type">${rule.type}</td>
      <td data-label="Active">${rule.is_active ? "Yes" : "No"}</td>
      <td data-label="Version">${formatNumber(rule.version)}</td>
      <td data-label="Action">
        <div class="action-pack">
          <button class="secondary" data-toggle="${rule.id}">${rule.is_active ? "Disable" : "Enable"}</button>
          <button class="ghost" data-tune="${rule.id}">Tune</button>
        </div>
      </td>
    `;
    body.appendChild(row);
  }

  body.querySelectorAll("button[data-toggle]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      try {
        await request(`/api/rules/${btn.dataset.toggle}/toggle`, { method: "PATCH" });
        showToast(`Rule #${btn.dataset.toggle} status updated.`);
        await refreshAll();
      } catch (error) {
        showToast(error.message, "error");
      }
    });
  });

  body.querySelectorAll("button[data-tune]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const rule = state.rules.find((item) => item.id === Number(btn.dataset.tune));
      populateRuleForm(rule);
      showToast(`Loaded Rule #${btn.dataset.tune} in tuning form.`);
    });
  });
}

async function saveRuleTuning(event) {
  event.preventDefault();
  const ruleId = Number(document.getElementById("rule-select").value);
  const existing = state.rules.find((rule) => rule.id === ruleId);
  if (!existing) {
    showToast("Select a rule first.", "error");
    return;
  }

  const config = { ...(existing.config || {}) };
  const threshold = readNumberInput("rule-threshold");
  const maxCount = readNumberInput("rule-max-count");
  const windowMinutes = readNumberInput("rule-window-minutes");
  const dailyLimit = readNumberInput("rule-daily-limit");
  const dedupWindowMinutes = readNumberInput("rule-dedup-window");

  if (threshold !== undefined) config.threshold = threshold;
  if (maxCount !== undefined) config.maxCount = maxCount;
  if (windowMinutes !== undefined) config.windowMinutes = windowMinutes;
  if (dailyLimit !== undefined) config.dailyLimit = dailyLimit;
  if (dedupWindowMinutes !== undefined) config.dedupWindowMinutes = dedupWindowMinutes;

  try {
    await request(`/api/rules/${ruleId}`, {
      method: "PUT",
      body: JSON.stringify({
        name: document.getElementById("rule-name").value.trim() || existing.name,
        severity: document.getElementById("rule-severity").value,
        is_active: !!existing.is_active,
        config,
      }),
    });
    showToast(`Rule #${ruleId} tuning saved.`);
    await refreshAll();
  } catch (error) {
    showToast(error.message, "error");
  }
}

async function loadHistory() {
  const body = document.getElementById("history-body");
  body.innerHTML = emptyRow(6, "Loading history...");

  const selected = document.getElementById("history-status-filter").value;
  const data = await request("/api/alerts/history");
  state.history = selected ? data.items.filter((item) => item.status === selected) : data.items;

  if (!state.history.length) {
    body.innerHTML = emptyRow(6, "No history records.");
    return;
  }

  body.innerHTML = "";
  for (const alert of state.history) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td data-label="ID">${formatNumber(alert.id)}</td>
      <td data-label="Rule">${alert.rule_name}</td>
      <td data-label="Status">${statusBadge(alert.status)}</td>
      <td data-label="Severity">${alert.severity}</td>
      <td data-label="Resolved At">${formatDateTime(alert.updated_at)}</td>
      <td data-label="Reason">${alert.resolution_reason || "-"}</td>
    `;
    body.appendChild(row);
  }
}

async function loadPerformance() {
  const body = document.getElementById("performance-body");
  body.innerHTML = emptyRow(5, "Loading performance...");
  const data = await request("/api/rules/performance");
  state.performance = data.items || [];

  if (!state.performance.length) {
    body.innerHTML = emptyRow(5, "No performance data yet.");
    return;
  }

  body.innerHTML = "";
  for (const row of state.performance) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td data-label="Rule">${row.name}</td>
      <td data-label="Total Alerts">${formatNumber(row.total_alerts)}</td>
      <td data-label="Resolved">${formatNumber(row.resolved_alerts)}</td>
      <td data-label="Dismissed Rate">${formatPercent(row.dismissed_rate)}</td>
      <td data-label="Conversion Rate">${formatPercent(row.conversion_rate)}</td>
    `;
    body.appendChild(tr);
  }
}

async function refreshAll() {
  const refreshButton = document.getElementById("refresh-all");
  if (refreshButton) {
    refreshButton.disabled = true;
  }
  try {
    await Promise.all([
      loadSummary(),
      loadAlerts(),
      loadRules(),
      loadPerformance(),
      loadTransactions(),
      loadHistory(),
    ]);

    if (state.selectedAlertId) {
      await loadAlertDetail(state.selectedAlertId);
    }
    renderIntelligence();
    setLastSync();
  } finally {
    if (refreshButton) {
      refreshButton.disabled = false;
    }
  }
}

async function seedDemoAlerts() {
  const button = document.getElementById("seed-demo");
  if (button) {
    button.disabled = true;
    button.textContent = "Generating...";
  }

  const now = Date.now();
  const baseAccount = `DEMO-${Math.floor(now / 1000)}`;
  const template = [
    { account_id: baseAccount, payee_id: "PAYEE-NEW-01", amount: 18000, direction: "DEBIT", occurred_at: new Date(now - 6 * 60 * 1000).toISOString() },
    { account_id: baseAccount, payee_id: "PAYEE-NEW-01", amount: 1200, direction: "DEBIT", occurred_at: new Date(now - 5 * 60 * 1000).toISOString() },
    { account_id: baseAccount, payee_id: "PAYEE-NEW-01", amount: 1300, direction: "DEBIT", occurred_at: new Date(now - 4 * 60 * 1000).toISOString() },
    { account_id: baseAccount, payee_id: "PAYEE-NEW-01", amount: 1400, direction: "DEBIT", occurred_at: new Date(now - 3 * 60 * 1000).toISOString() },
    { account_id: baseAccount, payee_id: "PAYEE-NEW-01", amount: 1500, direction: "DEBIT", occurred_at: new Date(now - 2 * 60 * 1000).toISOString() },
    { account_id: baseAccount, payee_id: "PAYEE-NEW-01", amount: 1600, direction: "DEBIT", occurred_at: new Date(now - 1 * 60 * 1000).toISOString() },
    { account_id: `${baseAccount}-DL`, payee_id: "PAYEE-DL-01", amount: 51000, direction: "DEBIT", occurred_at: new Date(now - 30 * 1000).toISOString() },
  ];

  try {
    for (const tx of template) {
      await request("/api/transactions", {
        method: "POST",
        body: JSON.stringify(tx),
      });
    }
    showToast("Demo alerts generated successfully.");
    await refreshAll();
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    if (button) {
      button.disabled = false;
      button.textContent = "Generate Demo Alerts";
    }
  }
}

async function resetDemoData() {
  const confirmed = window.confirm("This will delete all DEMO-* transactions and related alerts. Continue?");
  if (!confirmed) {
    return;
  }

  const button = document.getElementById("reset-demo");
  if (button) {
    button.disabled = true;
    button.textContent = "Resetting...";
  }

  try {
    const result = await request("/api/demo/reset", { method: "POST" });
    const alertsDeleted = result.deleted?.alerts || 0;
    showToast(`Reset complete. Removed ${alertsDeleted} demo alerts.`);
    state.selectedAlertId = null;
    document.getElementById("alert-detail").className = "detail-panel empty";
    document.getElementById("alert-detail").textContent = "Select an alert to inspect its timeline and linked transaction.";
    await refreshAll();
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    if (button) {
      button.disabled = false;
      button.textContent = "Reset Demo Data";
    }
  }
}

async function acknowledgeAllOpen() {
  const button = document.getElementById("ack-open");
  button.disabled = true;
  try {
    const openAlerts = await request("/api/alerts?status=OPEN");
    if (!openAlerts.items.length) {
      showToast("No OPEN alerts to acknowledge.");
      return;
    }

    for (const alert of openAlerts.items) {
      await patchAlertStatus(alert.id, "ACKNOWLEDGED", "Bulk acknowledge action");
    }
    showToast(`Acknowledged ${openAlerts.items.length} alerts.`);
    await refreshAll();
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    button.disabled = false;
  }
}

async function dismissLowSeverityOpen() {
  const button = document.getElementById("dismiss-low");
  button.disabled = true;
  try {
    const lowAlerts = await request("/api/alerts?status=OPEN&severity=LOW");
    if (!lowAlerts.items.length) {
      showToast("No LOW OPEN alerts to dismiss.");
      return;
    }

    for (const alert of lowAlerts.items) {
      await patchAlertStatus(alert.id, "DISMISSED", "Bulk low-severity dismissal", "Low severity bulk dismissal");
    }
    showToast(`Dismissed ${lowAlerts.items.length} low-severity alerts.`);
    await refreshAll();
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    button.disabled = false;
  }
}

function bindSeverityChips() {
  const chips = document.querySelectorAll(".chip[data-severity]");
  chips.forEach((chip) => {
    chip.addEventListener("click", async () => {
      state.selectedSeverity = chip.dataset.severity || "";
      chips.forEach((c) => c.classList.toggle("active", c === chip));
      await loadAlerts();
      renderIntelligence();
    });
  });
}

document.getElementById("refresh-alerts").addEventListener("click", async () => {
  await loadAlerts();
  renderIntelligence();
});
document.getElementById("alert-status-filter").addEventListener("change", async () => {
  await loadAlerts();
  renderIntelligence();
});
document.getElementById("seed-demo").addEventListener("click", seedDemoAlerts);
document.getElementById("reset-demo").addEventListener("click", resetDemoData);
document.getElementById("ack-open").addEventListener("click", acknowledgeAllOpen);
document.getElementById("dismiss-low").addEventListener("click", dismissLowSeverityOpen);
document.getElementById("refresh-all").addEventListener("click", async () => {
  try {
    await refreshAll();
    showToast("Dashboard refreshed.");
  } catch (error) {
    showToast(error.message, "error");
  }
});

document.getElementById("tx-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  const payload = {
    account_id: form.get("account_id"),
    payee_id: form.get("payee_id"),
    amount: Number(form.get("amount")),
    direction: form.get("direction"),
  };

  try {
    const result = await request("/api/transactions", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    document.getElementById("tx-result").textContent = JSON.stringify(result, null, 2);
    event.target.reset();
    showToast(`Transaction #${result.transaction.id} processed successfully.`);
    await refreshAll();
  } catch (error) {
    document.getElementById("tx-result").textContent = error.message;
    showToast(error.message, "error");
  }
});

document.getElementById("rule-select").addEventListener("change", (event) => {
  const rule = state.rules.find((item) => item.id === Number(event.target.value));
  populateRuleForm(rule);
});
document.getElementById("rule-tuning-form").addEventListener("submit", saveRuleTuning);

document.getElementById("tx-search").addEventListener("input", () => {
  clearTimeout(txSearchTimer);
  txSearchTimer = setTimeout(() => {
    loadTransactions().catch((error) => showToast(error.message, "error"));
  }, 260);
});
document.getElementById("tx-account-filter").addEventListener("input", () => {
  clearTimeout(txSearchTimer);
  txSearchTimer = setTimeout(() => {
    loadTransactions().catch((error) => showToast(error.message, "error"));
  }, 260);
});
document.getElementById("refresh-transactions").addEventListener("click", () => {
  loadTransactions().catch((error) => showToast(error.message, "error"));
});

document.getElementById("history-status-filter").addEventListener("change", () => {
  loadHistory().catch((error) => showToast(error.message, "error"));
});
document.getElementById("refresh-history").addEventListener("click", () => {
  loadHistory().catch((error) => showToast(error.message, "error"));
});

refreshAll().catch((error) => {
  document.getElementById("tx-result").textContent = error.message;
});

bindSeverityChips();
