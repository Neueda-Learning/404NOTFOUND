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

let toastTimer;
let selectedSeverity = "";

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
  }, 2600);
}

function statusAction(status) {
  if (status === "OPEN") return "ACKNOWLEDGED";
  if (status === "ACKNOWLEDGED") return "INVESTIGATING";
  if (status === "INVESTIGATING") return "CLOSED";
  return null;
}

function statusCell(status) {
  return `<span class="badge ${status}">${status}</span>`;
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

async function loadSummary() {
  const data = await request("/api/dashboard/summary");
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

async function loadAlerts() {
  const status = document.getElementById("alert-status-filter").value;
  const query = new URLSearchParams();
  if (status) {
    query.set("status", status);
  }
  if (selectedSeverity) {
    query.set("severity", selectedSeverity);
  }
  const url = query.toString() ? `/api/alerts?${query.toString()}` : "/api/alerts";
  const body = document.getElementById("alerts-body");
  body.innerHTML = emptyRow(6, "Loading alerts...");
  const data = await request(url);

  if (!data.items.length) {
    body.innerHTML = emptyRow(6, "No alerts for current filter.");
    return;
  }

  body.innerHTML = "";

  for (const alert of data.items) {
    const row = document.createElement("tr");
    const next = statusAction(alert.status);
    row.innerHTML = `
      <td data-label="ID">${formatNumber(alert.id)}</td>
      <td data-label="Rule">${alert.rule_name}</td>
      <td data-label="Status">${statusCell(alert.status)}</td>
      <td data-label="Severity">${alert.severity}</td>
      <td data-label="Score">${formatNumber(alert.score)}</td>
      <td data-label="Action">
        ${next ? `<button data-id="${alert.id}" data-next="${next}">${next}</button>` : "-"}
      </td>
    `;
    body.appendChild(row);
  }

  body.querySelectorAll("button[data-id]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      try {
        await patchAlertStatus(btn.dataset.id, btn.dataset.next, "Updated from dashboard");
        showToast(`Alert #${btn.dataset.id} moved to ${btn.dataset.next}.`);
        await refreshAll();
      } catch (error) {
        showToast(error.message, "error");
      }
    });
  });
}

async function loadRules() {
  const body = document.getElementById("rules-body");
  body.innerHTML = emptyRow(6, "Loading rules...");
  const data = await request("/api/rules");

  if (!data.items.length) {
    body.innerHTML = emptyRow(6, "No rules found.");
    return;
  }

  body.innerHTML = "";

  for (const rule of data.items) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td data-label="ID">${formatNumber(rule.id)}</td>
      <td data-label="Name">${rule.name}</td>
      <td data-label="Type">${rule.type}</td>
      <td data-label="Active">${rule.is_active ? "Yes" : "No"}</td>
      <td data-label="Version">${formatNumber(rule.version)}</td>
      <td data-label="Action"><button class="secondary" data-toggle="${rule.id}">${rule.is_active ? "Disable" : "Enable"}</button></td>
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
}

async function loadPerformance() {
  const body = document.getElementById("performance-body");
  body.innerHTML = emptyRow(5, "Loading performance...");
  const data = await request("/api/rules/performance");

  if (!data.items.length) {
    body.innerHTML = emptyRow(5, "No performance data yet.");
    return;
  }

  body.innerHTML = "";

  for (const row of data.items) {
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
    await Promise.all([loadSummary(), loadAlerts(), loadRules(), loadPerformance()]);
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
      selectedSeverity = chip.dataset.severity || "";
      chips.forEach((c) => c.classList.toggle("active", c === chip));
      await loadAlerts();
    });
  });
}

document.getElementById("refresh-alerts").addEventListener("click", loadAlerts);
document.getElementById("alert-status-filter").addEventListener("change", loadAlerts);
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

refreshAll().catch((error) => {
  document.getElementById("tx-result").textContent = error.message;
});

bindSeverityChips();
