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

function statusAction(status) {
  if (status === "OPEN") return "ACKNOWLEDGED";
  if (status === "ACKNOWLEDGED") return "INVESTIGATING";
  if (status === "INVESTIGATING") return "CLOSED";
  return null;
}

function statusCell(status) {
  return `<span class="badge ${status}">${status}</span>`;
}

async function loadSummary() {
  const data = await request("/api/dashboard/summary");
  const summary = document.getElementById("summary");
  summary.innerHTML = "";

  const blocks = [
    ["OPEN", data.status_counts.OPEN || 0],
    ["ACKNOWLEDGED", data.status_counts.ACKNOWLEDGED || 0],
    ["INVESTIGATING", data.status_counts.INVESTIGATING || 0],
    ["CLOSED", data.status_counts.CLOSED || 0],
    ["DISMISSED", data.status_counts.DISMISSED || 0],
    ["Alerts Today", data.alerts_today],
    ["Avg Resolve (min)", data.avg_resolution_minutes],
  ];

  for (const [label, value] of blocks) {
    const div = document.createElement("div");
    div.className = "summary-item";
    div.innerHTML = `<span>${label}</span><b>${value}</b>`;
    summary.appendChild(div);
  }
}

async function loadAlerts() {
  const status = document.getElementById("alert-status-filter").value;
  const url = status ? `/api/alerts?status=${encodeURIComponent(status)}` : "/api/alerts";
  const data = await request(url);
  const body = document.getElementById("alerts-body");
  body.innerHTML = "";

  for (const alert of data.items) {
    const row = document.createElement("tr");
    const next = statusAction(alert.status);
    row.innerHTML = `
      <td>${alert.id}</td>
      <td>${alert.rule_name}</td>
      <td>${statusCell(alert.status)}</td>
      <td>${alert.severity}</td>
      <td>${alert.score}</td>
      <td>
        ${next ? `<button data-id="${alert.id}" data-next="${next}">${next}</button>` : "-"}
      </td>
    `;
    body.appendChild(row);
  }

  body.querySelectorAll("button[data-id]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      await request(`/api/alerts/${btn.dataset.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: btn.dataset.next, note: "Updated from dashboard" }),
      });
      await refreshAll();
    });
  });
}

async function loadRules() {
  const data = await request("/api/rules");
  const body = document.getElementById("rules-body");
  body.innerHTML = "";

  for (const rule of data.items) {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${rule.id}</td>
      <td>${rule.name}</td>
      <td>${rule.type}</td>
      <td>${rule.is_active ? "Yes" : "No"}</td>
      <td>${rule.version}</td>
      <td><button class="secondary" data-toggle="${rule.id}">${rule.is_active ? "Disable" : "Enable"}</button></td>
    `;
    body.appendChild(row);
  }

  body.querySelectorAll("button[data-toggle]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      await request(`/api/rules/${btn.dataset.toggle}/toggle`, { method: "PATCH" });
      await refreshAll();
    });
  });
}

async function loadPerformance() {
  const data = await request("/api/rules/performance");
  const body = document.getElementById("performance-body");
  body.innerHTML = "";

  for (const row of data.items) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${row.name}</td>
      <td>${row.total_alerts}</td>
      <td>${row.resolved_alerts}</td>
      <td>${row.dismissed_rate}</td>
      <td>${row.conversion_rate}</td>
    `;
    body.appendChild(tr);
  }
}

async function refreshAll() {
  await Promise.all([loadSummary(), loadAlerts(), loadRules(), loadPerformance()]);
}

document.getElementById("refresh-alerts").addEventListener("click", loadAlerts);
document.getElementById("alert-status-filter").addEventListener("change", loadAlerts);

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
    await refreshAll();
  } catch (error) {
    document.getElementById("tx-result").textContent = error.message;
  }
});

refreshAll().catch((error) => {
  document.getElementById("tx-result").textContent = error.message;
});
