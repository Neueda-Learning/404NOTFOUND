import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import type { AlertSeverity, AlertStatus, TransactionStatus, TransactionType } from '../types';

dayjs.extend(utc);

export const formatTime = (iso?: string): string => {
  if (!iso) return '-';
  return dayjs(iso).format('YYYY-MM-DD HH:mm:ss');
};

export const formatTimeShort = (iso?: string): string => {
  if (!iso) return '-';
  return dayjs(iso).format('MM-DD HH:mm');
};

export const formatAmount = (amount?: number, currency?: string): string => {
  if (amount == null) return '-';
  const formatted = amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return currency ? `${formatted} ${currency}` : formatted;
};

export const severityColor: Record<AlertSeverity, string> = {
  HIGH: 'red',
  MEDIUM: 'orange',
  LOW: 'blue',
};

export const statusColor: Record<AlertStatus, string> = {
  OPEN: 'red',
  ACKNOWLEDGED: 'orange',
  INVESTIGATING: 'blue',
  CLOSED: 'green',
  DISMISSED: 'default',
};

export const txStatusColor: Record<TransactionStatus, string> = {
  COMPLETED: 'green',
  PENDING: 'orange',
  FAILED: 'red',
  CANCELLED: 'default',
  REVERSED: 'purple',
};

export const txTypeColor: Record<TransactionType, string> = {
  DEBIT: 'red',
  CREDIT: 'green',
  TRANSFER: 'blue',
  REFUND: 'purple',
};

export const resolutionCodeLabels: Record<string, string> = {
  TRUE_POSITIVE: 'True Positive',
  FALSE_POSITIVE: 'False Positive',
  LEGITIMATE_ACTIVITY: 'Legitimate Activity',
  INSUFFICIENT_INFORMATION: 'Insufficient Information',
  ESCALATED: 'Escalated',
};

export const ruleTypeLabel: Record<string, string> = {
  AMOUNT_THRESHOLD: 'Amount Threshold',
  VELOCITY: 'Velocity',
  NEW_PAYEE: 'New Payee',
  DAILY_LIMIT: 'Daily Limit',
};
