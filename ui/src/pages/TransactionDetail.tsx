import { useEffect, useState } from 'react';
import {
  Descriptions, Tag, Card, Table, Typography,
  Spin, Result, Button, Space,
} from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useParams, useNavigate, Link } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { getTransaction } from '../api/transactions';
import { getAlert } from '../api/alerts';
import type { Transaction, AlertDetail } from '../types';
import {
  formatTime, formatAmount, txStatusColor, txTypeColor,
  severityColor, statusColor,
} from '../utils/format';

const { Title, Text } = Typography;

export default function TransactionDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [tx, setTx] = useState<Transaction | null>(null);
  const [alerts, setAlerts] = useState<AlertDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);

    getTransaction(id)
      .then(transaction => {
        setTx(transaction);
        // Fetch all related alerts in parallel
        if (transaction.alertIds && transaction.alertIds.length > 0) {
          return Promise.all(transaction.alertIds.map(aid =>
            getAlert(aid).catch(() => null)
          )).then(results => {
            setAlerts(results.filter(Boolean) as AlertDetail[]);
            return transaction;
          });
        }
        setAlerts([]);
        return transaction;
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" />
        <div style={{ marginTop: 16, color: '#999' }}>Loading transaction...</div>
      </div>
    );
  }

  if (error || !tx) {
    return (
      <Result
        status="error"
        title="Failed to load transaction"
        subTitle={error || 'Transaction not found'}
        extra={
          <Space>
            <Button onClick={() => navigate('/transactions')}>Back to Transactions</Button>
            <Button type="primary" onClick={() => window.location.reload()}>Retry</Button>
          </Space>
        }
      />
    );
  }

  const alertColumns: ColumnsType<AlertDetail> = [
    {
      title: 'Alert ID',
      dataIndex: 'alertId',
      width: 180,
      render: (alertId: string) => (
        <Link
          to={`/alerts/${alertId}`}
          style={{ fontFamily: 'monospace', fontSize: 15 }}
        >
          {alertId}
        </Link>
      ),
    },
    {
      title: 'Title',
      dataIndex: 'title',
      ellipsis: true,
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      width: 90,
      render: v => (
        <Tag color={severityColor[v as keyof typeof severityColor] || 'default'}>
          {v}
        </Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 130,
      render: v => (
        <Tag color={statusColor[v as keyof typeof statusColor] || 'default'}>
          {v}
        </Tag>
      ),
    },
    {
      title: 'Rule',
      dataIndex: 'ruleName',
      width: 160,
      ellipsis: true,
      render: v => v || '-',
    },
    {
      title: 'Created At',
      dataIndex: 'createdAt',
      width: 170,
      render: v => formatTime(v),
    },
  ];

  return (
    <div>
      {/* Back button */}
      <Button
        type="link"
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate('/transactions')}
        style={{ padding: 0, marginBottom: 16 }}
      >
        Back to Transactions
      </Button>

      <Title level={4} style={{ marginTop: 0 }}>
        Transaction: <Text code>{tx.transactionId}</Text>
      </Title>

      {/* Section 1: Summary */}
      <Card title="Transaction Summary" style={{ marginBottom: 16 }}>
        <Descriptions column={2} size="middle" bordered>
          <Descriptions.Item label="Transaction ID" span={2}>
            <Text code>{tx.transactionId}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color={txStatusColor[tx.status]}>{tx.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Amount">
            <Text strong>{formatAmount(tx.amount, tx.currency)}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Currency">{tx.currency}</Descriptions.Item>
          <Descriptions.Item label="Type">
            <Tag color={txTypeColor[tx.type]}>{tx.type}</Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Section 2: Details */}
      <Card title="Transaction Details" style={{ marginBottom: 16 }}>
        <Descriptions column={2} size="middle" bordered>
          <Descriptions.Item label="Transaction Time">{formatTime(tx.transactionTime)}</Descriptions.Item>
          <Descriptions.Item label="Received Time">{formatTime(tx.receivedAt)}</Descriptions.Item>
          <Descriptions.Item label="Evaluated Time">{formatTime(tx.evaluatedAt)}</Descriptions.Item>
          <Descriptions.Item label="Evaluation Mode">{tx.evaluationMode || '-'}</Descriptions.Item>
          <Descriptions.Item label="Version">{tx.version}</Descriptions.Item>
          <Descriptions.Item label="Late Arrival">
            <Tag color={tx.lateArrival ? 'orange' : 'default'}>{tx.lateArrival ? 'Yes' : 'No'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Payment Channel">{tx.paymentChannel || '-'}</Descriptions.Item>
          <Descriptions.Item label="Country">{tx.country || '-'}</Descriptions.Item>
          <Descriptions.Item label="Description" span={2}>{tx.description || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Section 3: Account */}
      <Card title="Account Information" style={{ marginBottom: 16 }}>
        <Descriptions column={2} size="middle" bordered>
          <Descriptions.Item label="Account ID">{tx.accountId}</Descriptions.Item>
          <Descriptions.Item label="Payee ID">{tx.payeeId || '-'}</Descriptions.Item>
          <Descriptions.Item label="Payee Name" span={2}>{tx.payeeName || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Section 4: Alerts */}
      <Card
        title={`Alerts (${alerts.length})`}
        style={{ marginBottom: 16 }}
      >
        {alerts.length > 0 ? (
          <Table<AlertDetail>
            columns={alertColumns}
            dataSource={alerts}
            rowKey="alertId"
            size="middle"
            pagination={false}
            scroll={{ x: 900 }}
          />
        ) : (
          <div style={{
            padding: 32,
            textAlign: 'center',
            color: '#999',
            background: '#fafafa',
            borderRadius: 6,
          }}>
            No alerts triggered by this transaction
          </div>
        )}
      </Card>
    </div>
  );
}
