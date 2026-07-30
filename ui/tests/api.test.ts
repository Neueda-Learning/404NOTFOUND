import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockClient } = vi.hoisted(() => ({
  mockClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('../src/api/client', () => ({
  default: mockClient,
}));

import {
  acknowledgeAlert,
  closeAlert,
  dismissAlert,
  getAlert,
  investigateAlert,
  searchAlerts,
} from '../src/api/alerts';
import { getDashboardSummary } from '../src/api/dashboard';
import { createRule, deleteRule, getRule, listRules, toggleRule, updateRule } from '../src/api/rules';
import { createTransaction, getTransaction, searchTransactions } from '../src/api/transactions';

describe('ui api modules', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('searchAlerts calls GET with params and returns data', async () => {
    const payload = { items: [], total: 0, page: 0, size: 20 };
    mockClient.get.mockResolvedValueOnce({ data: payload });

    await expect(searchAlerts({ status: 'OPEN', page: 1, size: 20 })).resolves.toEqual(payload);
    expect(mockClient.get).toHaveBeenCalledWith('/alerts', {
      params: { status: 'OPEN', page: 1, size: 20 },
    });
  });

  it('alert detail and actions use expected endpoints', async () => {
    const alertPayload = { id: 'A-1' };
    mockClient.get.mockResolvedValueOnce({ data: alertPayload });
    await expect(getAlert('A-1')).resolves.toEqual(alertPayload);
    expect(mockClient.get).toHaveBeenCalledWith('/alerts/A-1');

    const req = { operator: 'qa', note: 'reviewed' };
    const actionResult = { id: 'A-1', status: 'CLOSED' };
    mockClient.post.mockResolvedValue({ data: actionResult });

    await expect(acknowledgeAlert('A-1', req)).resolves.toEqual(actionResult);
    await expect(investigateAlert('A-1', req)).resolves.toEqual(actionResult);
    await expect(closeAlert('A-1', req)).resolves.toEqual(actionResult);
    await expect(dismissAlert('A-1', req)).resolves.toEqual(actionResult);

    expect(mockClient.post).toHaveBeenCalledWith('/alerts/A-1/acknowledge', req);
    expect(mockClient.post).toHaveBeenCalledWith('/alerts/A-1/investigate', req);
    expect(mockClient.post).toHaveBeenCalledWith('/alerts/A-1/close', req);
    expect(mockClient.post).toHaveBeenCalledWith('/alerts/A-1/dismiss', req);
  });

  it('dashboard summary uses summary endpoint', async () => {
    const summary = { openAlerts: 2 };
    mockClient.get.mockResolvedValueOnce({ data: summary });

    await expect(getDashboardSummary()).resolves.toEqual(summary);
    expect(mockClient.get).toHaveBeenCalledWith('/dashboard/summary');
  });

  it('rules endpoints call expected http methods', async () => {
    const rule = { id: 12, name: 'High Amount' };
    mockClient.get.mockResolvedValueOnce({ data: [rule] });
    mockClient.get.mockResolvedValueOnce({ data: rule });
    mockClient.post.mockResolvedValueOnce({ data: rule });
    mockClient.put.mockResolvedValueOnce({ data: { ...rule, name: 'Updated' } });
    mockClient.patch.mockResolvedValueOnce({ data: { ...rule, enabled: false } });
    mockClient.delete.mockResolvedValueOnce({ status: 204 });

    await expect(listRules()).resolves.toEqual([rule]);
    await expect(getRule(12)).resolves.toEqual(rule);
    await expect(createRule({ name: 'High Amount' })).resolves.toEqual(rule);
    await expect(updateRule(12, { name: 'Updated' })).resolves.toEqual({ ...rule, name: 'Updated' });
    await expect(toggleRule(12)).resolves.toEqual({ ...rule, enabled: false });
    await expect(deleteRule(12)).resolves.toEqual({ status: 204 });

    expect(mockClient.get).toHaveBeenCalledWith('/rules');
    expect(mockClient.get).toHaveBeenCalledWith('/rules/12');
    expect(mockClient.post).toHaveBeenCalledWith('/rules', { name: 'High Amount' });
    expect(mockClient.put).toHaveBeenCalledWith('/rules/12', { name: 'Updated' });
    expect(mockClient.patch).toHaveBeenCalledWith('/rules/12/toggle');
    expect(mockClient.delete).toHaveBeenCalledWith('/rules/12');
  });

  it('transactions endpoints call expected http methods', async () => {
    const txPayload = { items: [{ transactionId: 'T-1' }], total: 1, page: 0, size: 10 };
    const tx = { transactionId: 'T-1', amount: 100 };
    const createReq = {
      transactionId: 'T-2',
      accountId: 'ACCT-1',
      type: 'DEBIT',
      amount: 100,
      currency: 'USD',
      status: 'COMPLETED',
      transactionTime: '2026-01-01T00:00:00Z',
    };

    mockClient.get.mockResolvedValueOnce({ data: txPayload });
    mockClient.get.mockResolvedValueOnce({ data: tx });
    mockClient.post.mockResolvedValueOnce({ data: tx });

    await expect(searchTransactions({ accountId: 'ACCT-1', page: 0, size: 10 })).resolves.toEqual(txPayload);
    await expect(getTransaction('T-1')).resolves.toEqual(tx);
    await expect(createTransaction(createReq)).resolves.toEqual(tx);

    expect(mockClient.get).toHaveBeenCalledWith('/transactions', {
      params: { accountId: 'ACCT-1', page: 0, size: 10 },
    });
    expect(mockClient.get).toHaveBeenCalledWith('/transactions/T-1');
    expect(mockClient.post).toHaveBeenCalledWith('/transactions', createReq);
  });
});
