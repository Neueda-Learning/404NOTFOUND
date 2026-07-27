import { useEffect, useState } from 'react';
import {
  Row, Col, Card, Tag, Button, Typography, Space, Spin, Alert as AntAlert,
  Descriptions, Steps, Input, Select, Modal, Table, Timeline, Progress,
  notification, Divider,
} from 'antd';
import {
  ArrowLeftOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import {
  getAlert, acknowledgeAlert, investigateAlert, closeAlert, dismissAlert,
} from '../api/alerts';
import type { AlertDetail, AlertActionRequest, Transaction, ResolutionCode } from '../types';
import {
  formatTime, severityColor, statusColor, formatAmount,
  txStatusColor, txTypeColor, ruleTypeLabel, resolutionCodeLabels,
} from '../utils/format';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const { Option } = Select;

const RESOLUTION_OPTIONS: ResolutionCode[] = [
  'TRUE_POSITIVE', 'FALSE_POSITIVE', 'LEGITIMATE_ACTIVITY',
  'INSUFFICIENT_INFORMATION', 'ESCALATED',
];

function StatusBreadcrumb({ alertId }: { alertId: string }) {
  const navigate = useNavigate();
  return (
    <Space split="/">
      <Button type="link" style={{ padding: 0 }} onClick={() => navigate('/')}>Dashboard</Button>
      <Button type="link" style={{ padding: 0 }} onClick={() => navigate('/alerts')}>Alerts</Button>
      <Text>{alertId}</Text>
    </Space>
  );
}

function AlertSummaryCard({ alert, onAction }: { alert: AlertDetail; onAction: (action: string) => void }) {
  const actionButtons: { label: string; action: string; danger?: boolean; type?: 'primary' | 'default' }[] = [];

  if (alert.status === 'OPEN') {
    actionButtons.push({ label: 'Acknowledge', action: 'acknowledge', type: 'primary' });
    actionButtons.push({ label: 'Dismiss', action: 'dismiss', danger: true });
  } else if (alert.status === 'ACKNOWLEDGED') {
    actionButtons.push({ label: 'Start Investigation', action: 'investigate', type: 'primary' });
    actionButtons.push({ label: 'Dismiss', action: 'dismiss', danger: true });
  } else if (alert.status === 'INVESTIGATING') {
    actionButtons.push({ label: 'Close Alert', action: 'close', type: 'primary' });
    actionButtons.push({ label: 'Dismiss', action: 'dismiss', danger: true });
  }

  return (
    <Card style={{ marginBottom: 24 }}>
      <Row align="middle" gutter={24}>
        <Col flex="auto">
          <Title level={4} style={{ margin: 0 }}>{alert.title}</Title>
          <Space style={{ marginTop: 4 }}>
            <Text code>{alert.alertId}</Text>
            <Text type="secondary">Account: {alert.accountId}</Text>
          </Space>
          {alert.description && <Paragraph type="secondary" style={{ margin: '8px 0 0', fontSize: 13 }}>{alert.description}</Paragraph>}
          <Space style={{ marginTop: 8 }}>
            <Text type="secondary">Created: {formatTime(alert.createdAt)}</Text>
            <Text type="secondary">Updated: {formatTime(alert.updatedAt)}</Text>
          </Space>
        </Col>

        <Col style={{ textAlign: 'center', minWidth: 160 }}>
          <Space direction="vertical" size={8}>
            <Tag color={statusColor[alert.status]} style={{ fontSize: 14, padding: '4px 12px' }}>{alert.status}</Tag>
            <Tag color={severityColor[alert.severity]} style={{ fontSize: 14, padding: '4px 12px' }}>{alert.severity}</Tag>
            <div>
              <Text type="secondary" style={{ fontSize: 12 }}>Risk Score</Text>
              <Progress
                type="circle"
                percent={alert.riskScore}
                size={60}
                strokeColor={alert.riskScore >= 75 ? '#ff4d4f' : alert.riskScore >= 50 ? '#faad14' : '#52c41a'}
                format={p => <span style={{ fontSize: 14, fontWeight: 700 }}>{p}</span>}
              />
            </div>
          </Space>
        </Col>

        {actionButtons.length > 0 && (
          <Col style={{ minWidth: 160 }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              {actionButtons.map(btn => (
                <Button
                  key={btn.action}
                  type={btn.type || 'default'}
                  danger={btn.danger}
                  style={{ width: '100%' }}
                  onClick={() => onAction(btn.action)}
                >
                  {btn.label}
                </Button>
              ))}
            </Space>
          </Col>
        )}
      </Row>
    </Card>
  );
}

function AlertInfoCard({ alert }: { alert: AlertDetail }) {
  return (
    <Card title="Alert Information" style={{ marginBottom: 16 }}>
      <Descriptions column={2} size="small" bordered>
        <Descriptions.Item label="Alert ID"><Text code>{alert.alertId}</Text></Descriptions.Item>
        <Descriptions.Item label="Title">{alert.title}</Descriptions.Item>
        <Descriptions.Item label="Description" span={2}>{alert.description || '-'}</Descriptions.Item>
        <Descriptions.Item label="Status"><Tag color={statusColor[alert.status]}>{alert.status}</Tag></Descriptions.Item>
        <Descriptions.Item label="Severity"><Tag color={severityColor[alert.severity]}>{alert.severity}</Tag></Descriptions.Item>
        <Descriptions.Item label="Risk Score">
          <Text strong style={{ color: alert.riskScore >= 75 ? '#ff4d4f' : '#faad14' }}>{alert.riskScore}/100</Text>
        </Descriptions.Item>
        <Descriptions.Item label="Account ID">{alert.accountId}</Descriptions.Item>
        <Descriptions.Item label="Transaction Count">{alert.transactionCount ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="Total Amount">{formatAmount(alert.totalAmount, alert.currency)}</Descriptions.Item>
        <Descriptions.Item label="Currency">{alert.currency || '-'}</Descriptions.Item>
        <Descriptions.Item label="First Transaction">{formatTime(alert.firstTransactionAt)}</Descriptions.Item>
        <Descriptions.Item label="Last Transaction">{formatTime(alert.lastTransactionAt)}</Descriptions.Item>
        <Descriptions.Item label="Created">{formatTime(alert.createdAt)}</Descriptions.Item>
        <Descriptions.Item label="Last Updated">{formatTime(alert.updatedAt)}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}

function TriggerRuleCard({ alert }: { alert: AlertDetail }) {
  return (
    <Card title="Triggered Rule" style={{ marginBottom: 16 }}>
      <Descriptions column={2} size="small" bordered>
        <Descriptions.Item label="Rule ID">{alert.ruleId || '-'}</Descriptions.Item>
        <Descriptions.Item label="Rule Name">{alert.ruleName || '-'}</Descriptions.Item>
        <Descriptions.Item label="Rule Type">
          {alert.ruleType ? <Tag>{ruleTypeLabel[alert.ruleType] || alert.ruleType}</Tag> : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="Actual Value">{alert.actualValue ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="Threshold">{alert.thresholdValue ?? '-'}</Descriptions.Item>
        {alert.timeWindowMinutes && (
          <Descriptions.Item label="Time Window">{alert.timeWindowMinutes} minutes</Descriptions.Item>
        )}
      </Descriptions>
      {alert.triggerReason && (
        <div style={{ marginTop: 12, padding: 12, background: '#fff7e6', borderRadius: 4, borderLeft: '4px solid #faad14' }}>
          <Text strong>Trigger Reason:</Text>
          <Paragraph style={{ margin: '4px 0 0' }}>{alert.triggerReason}</Paragraph>
        </div>
      )}
    </Card>
  );
}

function InvestigationPanel({
  alert, onSubmit, loading: submitting,
}: {
  alert: AlertDetail;
  onSubmit: (action: string, req: AlertActionRequest) => Promise<void>;
  loading: boolean;
}) {
  const [comment, setComment] = useState(alert.comment || '');
  const [resolutionCode, setResolutionCode] = useState<ResolutionCode | undefined>(alert.resolutionCode);
  const [resolutionNotes, setResolutionNotes] = useState(alert.resolutionNotes || '');
  const [commentError, setCommentError] = useState('');
  const [confirmAction, setConfirmAction] = useState<string | null>(null);

  const stepItems = [
    {
      title: 'Alert Created',
      description: formatTime(alert.createdAt),
      status: 'finish' as const,
    },
    {
      title: 'Acknowledged',
      description: alert.acknowledgedAt ? formatTime(alert.acknowledgedAt) : 'Pending',
      status: (alert.status === 'OPEN' ? 'wait' : 'finish') as 'wait' | 'finish' | 'process',
    },
    {
      title: 'Investigating',
      description: alert.investigatingAt ? formatTime(alert.investigatingAt) : 'Pending',
      status: (
        alert.status === 'OPEN' || alert.status === 'ACKNOWLEDGED' ? 'wait' :
        alert.status === 'INVESTIGATING' ? 'process' : 'finish'
      ) as 'wait' | 'process' | 'finish',
    },
    {
      title: alert.status === 'DISMISSED' ? 'Dismissed' : 'Investigation Complete',
      description: alert.closedAt ? formatTime(alert.closedAt) : alert.dismissedAt ? formatTime(alert.dismissedAt) : 'Pending',
      status: (
        alert.status === 'CLOSED' || alert.status === 'DISMISSED' ? 'finish' : 'wait'
      ) as 'wait' | 'finish',
    },
  ];

  const handleConfirm = async () => {
    if (!comment.trim()) {
      setCommentError('Comment is required');
      return;
    }
    await onSubmit(confirmAction!, {
      comment: comment.trim(),
      resolutionCode,
      resolutionNotes: resolutionNotes.trim() || undefined,
      expectedStatus: alert.status,
      expectedVersion: alert.version,
    });
    setConfirmAction(null);
  };

  return (
    <Card title="Investigation Panel" style={{ position: 'sticky', top: 80 }}>
      <Title level={5}>Progress</Title>
      <Steps direction="vertical" size="small" items={stepItems} style={{ marginBottom: 16 }} />

      <Divider />
      <Title level={5}>Investigation Notes</Title>
      <TextArea
        rows={4}
        placeholder="Enter investigation notes, findings, or handling instructions..."
        value={comment}
        onChange={e => { setComment(e.target.value); if (e.target.value.trim()) setCommentError(''); }}
        status={commentError ? 'error' : undefined}
      />
      {commentError && <Text type="danger" style={{ fontSize: 12 }}>{commentError}</Text>}

      {(confirmAction === 'close' || confirmAction === 'dismiss') && (
        <>
          <Divider />
          <Title level={5}>Resolution</Title>
          <Select
            placeholder="Select resolution code"
            style={{ width: '100%', marginBottom: 8 }}
            value={resolutionCode}
            onChange={setResolutionCode}
          >
            {RESOLUTION_OPTIONS.map(o => (
              <Option key={o} value={o}>{resolutionCodeLabels[o]}</Option>
            ))}
          </Select>
          <TextArea
            rows={3}
            placeholder="Enter resolution rationale and final conclusion..."
            value={resolutionNotes}
            onChange={e => setResolutionNotes(e.target.value)}
          />
        </>
      )}

      {/* Confirmation Modal */}
      <Modal
        open={!!confirmAction}
        title={<Space><ExclamationCircleOutlined style={{ color: '#faad14' }} /> Confirm Action</ Space>}
        onOk={handleConfirm}
        onCancel={() => setConfirmAction(null)}
        confirmLoading={submitting}
        okText="Confirm"
        cancelText="Cancel"
        okButtonProps={{ danger: confirmAction === 'dismiss' }}
      >
        <Descriptions column={1} size="small">
          <Descriptions.Item label="Current Status">
            <Tag color={statusColor[alert.status]}>{alert.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Target Status">
            <Tag color={
              confirmAction === 'acknowledge' ? 'orange' :
              confirmAction === 'investigate' ? 'blue' :
              confirmAction === 'close' ? 'green' : 'default'
            }>
              {confirmAction === 'acknowledge' ? 'ACKNOWLEDGED' :
               confirmAction === 'investigate' ? 'INVESTIGATING' :
               confirmAction === 'close' ? 'CLOSED' : 'DISMISSED'}
            </Tag>
          </Descriptions.Item>
          {resolutionCode && (
            <Descriptions.Item label="Resolution">{resolutionCodeLabels[resolutionCode]}</Descriptions.Item>
          )}
          {comment && <Descriptions.Item label="Comment">{comment}</Descriptions.Item>}
        </Descriptions>
      </Modal>

      {alert.status !== 'CLOSED' && alert.status !== 'DISMISSED' && (
        <div style={{ marginTop: 16 }}>
          {alert.status === 'OPEN' && (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" block onClick={() => setConfirmAction('acknowledge')} loading={submitting}>
                Acknowledge Alert
              </Button>
              <Button danger block onClick={() => setConfirmAction('dismiss')} loading={submitting}>
                Dismiss Alert
              </Button>
            </Space>
          )}
          {alert.status === 'ACKNOWLEDGED' && (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" block onClick={() => setConfirmAction('investigate')} loading={submitting}>
                Start Investigation
              </Button>
              <Button danger block onClick={() => setConfirmAction('dismiss')} loading={submitting}>
                Dismiss Alert
              </Button>
            </Space>
          )}
          {alert.status === 'INVESTIGATING' && (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" block onClick={() => setConfirmAction('close')} loading={submitting}>
                Close Alert
              </Button>
              <Button danger block onClick={() => setConfirmAction('dismiss')} loading={submitting}>
                Dismiss Alert
              </Button>
            </Space>
          )}
        </div>
      )}
    </Card>
  );
}

export default function AlertDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [alert, setAlert] = useState<AlertDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const load = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    getAlert(id)
      .then(setAlert)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [id]);

  const handleAction = async (action: string, req: AlertActionRequest) => {
    if (!id) return;
    setActionLoading(true);
    try {
      let updated: AlertDetail;
      if (action === 'acknowledge') updated = await acknowledgeAlert(id, req);
      else if (action === 'investigate') updated = await investigateAlert(id, req);
      else if (action === 'close') updated = await closeAlert(id, req);
      else updated = await dismissAlert(id, req);
      setAlert(updated);
      notification.success({ message: 'Action completed successfully' });
    } catch (e: any) {
      notification.error({ message: 'Action failed', description: e.message });
    } finally {
      setActionLoading(false);
    }
  };

  const txColumns: ColumnsType<Transaction> = [
    { title: 'Transaction ID', dataIndex: 'transactionId', width: 150, ellipsis: true, render: v => <Text code style={{ fontSize: 12 }}>{v}</Text> },
    { title: 'Payee', dataIndex: 'payeeName', ellipsis: true, render: (v, r) => v || r.payeeId || '-' },
    {
      title: 'Amount', dataIndex: 'amount', align: 'right', width: 130,
      render: (v, r) => <Text strong>{formatAmount(v, r.currency)}</Text>,
    },
    { title: 'Type', dataIndex: 'type', width: 90, render: (v: Transaction['type']) => <Tag color={txTypeColor[v]}>{v}</Tag> },
    { title: 'Status', dataIndex: 'status', width: 100, render: (v: Transaction['status']) => <Tag color={txStatusColor[v]}>{v}</Tag> },
    { title: 'Time', dataIndex: 'transactionTime', width: 150, render: v => formatTime(v) },
  ];

  if (loading) return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  if (error) return <AntAlert type="error" message={error} action={<Button onClick={load}>Retry</Button>} />;
  if (!alert) return null;

  return (
    <div>
      {/* Breadcrumb */}
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/alerts')}>Back to Alerts</Button>
        <StatusBreadcrumb alertId={alert.alertId} />
      </div>

      {/* Summary */}
      <AlertSummaryCard alert={alert} onAction={(_action) => {
        // For acknowledge/investigate the panel handles it
      }} />

      {/* Main Content - 2 column layout */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <AlertInfoCard alert={alert} />
          <TriggerRuleCard alert={alert} />
        </Col>
        <Col xs={24} lg={8}>
          <InvestigationPanel alert={alert} onSubmit={handleAction} loading={actionLoading} />
        </Col>
      </Row>

      {/* Related Transactions */}
      <Card
        title={`Related Transactions (${alert.transactions?.length || 0})`}
        style={{ marginBottom: 16 }}
      >
        {alert.transactions && alert.transactions.length > 0 ? (
          <Table<Transaction>
            columns={txColumns}
            dataSource={alert.transactions}
            rowKey="transactionId"
            size="small"
            pagination={false}
            expandable={{
              expandedRowRender: tx => (
                <Descriptions column={2} size="small">
                  <Descriptions.Item label="Account ID">{tx.accountId}</Descriptions.Item>
                  <Descriptions.Item label="Payee ID">{tx.payeeId || '-'}</Descriptions.Item>
                  <Descriptions.Item label="Description" span={2}>{tx.description || '-'}</Descriptions.Item>
                  <Descriptions.Item label="Full Transaction Time">{formatTime(tx.transactionTime)}</Descriptions.Item>
                  <Descriptions.Item label="Primary Trigger">
                    {tx.transactionId === alert.primaryTransactionId ? (
                      <Tag color="red">Yes</Tag>
                    ) : (
                      <Tag>No</Tag>
                    )}
                  </Descriptions.Item>
                </Descriptions>
              ),
            }}
          />
        ) : (
          <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>
            No related transactions found
          </div>
        )}
      </Card>

      {/* Status History */}
      <Card title="Status History">
        {alert.statusHistory && alert.statusHistory.length > 0 ? (
          <Timeline
            items={alert.statusHistory.map(h => ({
              color: h.toStatus === 'CLOSED' ? 'green' : h.toStatus === 'DISMISSED' ? 'gray' : 'blue',
              children: (
                <div style={{ paddingBottom: 8 }}>
                  <Space wrap>
                    <Text strong>{h.actionType}</Text>
                    {h.fromStatus && <Tag>{h.fromStatus}</Tag>}
                    <span>→</span>
                    <Tag color={statusColor[h.toStatus]}>{h.toStatus}</Tag>
                    <Text type="secondary" style={{ fontSize: 12 }}>{formatTime(h.changedAt)}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>by {h.changedBy}</Text>
                  </Space>
                  {h.comment && <div style={{ marginTop: 4 }}><Text type="secondary">{h.comment}</Text></div>}
                  {h.resolution && <Tag style={{ marginTop: 4 }}>{resolutionCodeLabels[h.resolution] || h.resolution}</Tag>}
                </div>
              ),
            }))}
          />
        ) : (
          <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>No history available</div>
        )}
      </Card>
    </div>
  );
}
