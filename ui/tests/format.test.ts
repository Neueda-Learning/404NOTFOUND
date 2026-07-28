import { describe, expect, it } from 'vitest';
import {
  formatAmount,
  formatTime,
  resolutionCodeLabels,
  ruleTypeLabel,
  severityColor,
  statusColor,
  txStatusColor,
  txTypeColor,
} from '../src/utils/format';

describe('format helpers', () => {
  it('formats amount with currency', () => {
    expect(formatAmount(1234.5, 'USD')).toBe('1,234.50 USD');
  });

  it('returns dash for missing amount', () => {
    expect(formatAmount(undefined, 'USD')).toBe('-');
  });

  it('formats iso time', () => {
    expect(formatTime('2026-07-28T00:00:00Z')).toBe('2026-07-28 00:00:00');
  });

  it('returns dash for missing time', () => {
    expect(formatTime(undefined)).toBe('-');
  });

  it('exposes expected status and severity mappings', () => {
    expect(statusColor.OPEN).toBe('#cf1322');
    expect(statusColor.CLOSED).toBe('#389e0d');
    expect(severityColor.HIGH).toBe('#cf1322');
    expect(severityColor.LOW).toBe('#1677ff');
  });

  it('exposes expected transaction mappings', () => {
    expect(txStatusColor.FAILED).toBe('#cf1322');
    expect(txStatusColor.COMPLETED).toBe('#389e0d');
    expect(txTypeColor.DEBIT).toBe('#cf1322');
    expect(txTypeColor.REFUND).toBe('#0f766e');
  });

  it('contains readable labels for rule and resolution codes', () => {
    expect(ruleTypeLabel.AMOUNT_THRESHOLD).toBe('Amount Threshold');
    expect(resolutionCodeLabels.TRUE_POSITIVE).toBe('True Positive');
  });
});
