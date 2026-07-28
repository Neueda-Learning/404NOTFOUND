// ==================== Enums ====================
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REVERSED';
export type TransactionType = 'DEBIT' | 'CREDIT' | 'TRANSFER' | 'REFUND';
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'INVESTIGATING' | 'CLOSED' | 'DISMISSED';
export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH';
export type RuleType = 'AMOUNT_THRESHOLD' | 'VELOCITY' | 'NEW_PAYEE' | 'DAILY_LIMIT';
export type ResolutionCode = 'TRUE_POSITIVE' | 'FALSE_POSITIVE' | 'LEGITIMATE_ACTIVITY' | 'INSUFFICIENT_INFORMATION' | 'ESCALATED';

// ==================== Transaction ====================
export interface Transaction {
  transactionId: string;
  accountId: string;
  payeeId?: string;
  payeeName?: string;
  type: TransactionType;
  amount: number;
  currency: string;
  status: TransactionStatus;
  transactionTime: string;
  receivedAt: string;
  evaluatedAt?: string;
  evaluationMode?: string;
  paymentChannel?: string;
  country?: string;
  description?: string;
  version: number;
  lateArrival?: boolean;
  hasAlert?: boolean;
  alertId?: string;
  alertStatus?: string;
  alertCount?: number;
  alertIds?: string[];
}

// ==================== Alert ====================
export interface AlertListItem {
  alertId: string;
  title: string;
  description?: string;
  accountId: string;
  status: AlertStatus;
  severity: AlertSeverity;
  riskScore: number;
  createdAt: string;
  updatedAt: string;
  acknowledgedAt?: string;
  investigatingAt?: string;
  closedAt?: string;
  dismissedAt?: string;
  primaryTransactionId?: string;
  primaryPayeeId?: string;
  totalAmount?: number;
  currency?: string;
  transactionCount?: number;
  firstTransactionAt?: string;
  lastTransactionAt?: string;
  resolutionCode?: ResolutionCode;
  resolution?: string;
  resolutionNotes?: string;
  comment?: string;
  version: number;
  ruleId?: string;
  ruleName?: string;
  ruleType?: string;
  triggerReason?: string;
  actualValue?: number;
  thresholdValue?: number;
  timeWindowMinutes?: number;
}

export interface AlertDetail extends AlertListItem {
  statusHistory: AlertHistoryItem[];
  transactions: Transaction[];
}

export interface AlertHistoryItem {
  id: number;
  changedAt: string;
  actionType: string;
  fromStatus?: AlertStatus;
  toStatus: AlertStatus;
  comment?: string;
  resolution?: ResolutionCode;
  changedBy: string;
}

// ==================== Rule ====================
export interface Rule {
  id: number;
  name: string;
  description?: string;
  ruleType: RuleType;
  severity: AlertSeverity;
  active: boolean;
  threshold?: number;
  maxTransactionCount?: number;
  timeWindowMinutes?: number;
  dailyLimit?: number;
  currency?: string;
  transactionTypes?: string[];
  version: number;
  createdAt: string;
  updatedAt: string;
  alertCount?: number;
  triggerCount?: number;
}

// ==================== Dashboard ====================
export interface AlertTrendPoint {
  date: string;
  high: number;
  medium: number;
  low: number;
  total: number;
}

export interface TopRule {
  ruleName: string;
  triggerCount: number;
}

export interface DashboardSummary {
  openAlerts: number;
  underInvestigation: number;
  todaysAlerts: number;
  highRiskAlerts: number;
  todaysTransactions: number;
  alertRate: number;
  alertTrend: AlertTrendPoint[];
  severityDistribution: Record<string, number>;
  topTriggeredRules: TopRule[];
}

// ==================== API Responses ====================
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ==================== Requests ====================
export interface AlertActionRequest {
  comment?: string;
  resolutionCode?: ResolutionCode;
  resolution?: string;
  resolutionNotes?: string;
  expectedStatus?: AlertStatus;
  expectedVersion?: number;
}
