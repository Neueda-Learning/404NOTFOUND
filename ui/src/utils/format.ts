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
  HIGH: '#cf1322',
  MEDIUM: '#d46b08',
  LOW: '#1677ff',
};

export const statusColor: Record<AlertStatus, string> = {
  OPEN: '#cf1322',
  ACKNOWLEDGED: '#d48806',
  INVESTIGATING: '#1677ff',
  CLOSED: '#389e0d',
  DISMISSED: '#6b7280',
};

export const txStatusColor: Record<TransactionStatus, string> = {
  COMPLETED: '#389e0d',
  PENDING: '#d48806',
  FAILED: '#cf1322',
  CANCELLED: '#6b7280',
  REVERSED: '#0f766e',
};

export const txTypeColor: Record<TransactionType, string> = {
  DEBIT: '#cf1322',
  CREDIT: '#389e0d',
  TRANSFER: '#1677ff',
  REFUND: '#0f766e',
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
