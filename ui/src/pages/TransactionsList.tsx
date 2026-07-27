import { useCallback, useEffect, useState } from 'react';
import {
  Table, Tag, Button, Input, Select, Row, Col,
  Typography, Alert as AntAlert, Modal, Descriptions,
} from 'antd';
import { SearchOutlined, ReloadOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { searchTransactions, getTransaction } from '../api/transactions';
import type { Transaction, TransactionStatus, TransactionType } from '../types';
import {
  formatTime, txStatusColor, txTypeColor, formatAmount, statusColor,
} from '../utils/format';

const { Title, Text } = Typography;
const { Option } = Select;

const statusOptions: TransactionStatus[] = ['PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED'];
const typeOptions: TransactionType[] = ['DEBIT', 'CREDIT', 'TRANSFER', 'REFUND'];

function TransactionDetailModal({
  tx, open, onClose, onViewAlert,
}: {
  tx: Transaction | null; open: boolean; onClose: () => void; onViewAlert: (alertId: string) => void;
}) {
  if (!tx) return null;
  return (
    <Modal
      open={open}
      title={`Transaction: ${tx.transactionId}`}
      onCancel={onClose}
      footer={<Button onClick={onClose}>Close</Button>}
      width={640}
    >
      {/* Section 1: Summary */}
      <Title level={5} style={{ marginTop: 0 }}>Transaction Summary</Title>
      <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
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
      </Descriptions>

      {/* Section 2: Details */}
      <Title level={5}>Transaction Details</Title>
      <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
        <Descriptions.Item label="Transaction Time">{formatTime(tx.transactionTime)}</Descriptions.Item>
        <Descriptions.Item label="Received Time">{formatTime(tx.receivedAt)}</Descriptions.Item>
        <Descriptions.Item label="Evaluated Time">{formatTime(tx.evaluatedAt)}</Descriptions.Item>
        <Descriptions.Item label="Evaluation Mode">{tx.evaluationMode || '-'}</Descriptions.Item>
        <Descriptions.Item label="Version">{tx.version}</Descriptions.Item>
        <Descriptions.Item label="Type">
          <Tag color={txTypeColor[tx.type]}>{tx.type}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Payment Channel">{tx.paymentChannel || '-'}</Descriptions.Item>
        <Descriptions.Item label="Country">{tx.country || '-'}</Descriptions.Item>
        <Descriptions.Item label="Description" span={2}>{tx.description || '-'}</Descriptions.Item>
      </Descriptions>

      {/* Section 3: Account */}
      <Title level={5}>Account Information</Title>
      <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
        <Descriptions.Item label="Account ID">{tx.accountId}</Descriptions.Item>
        <Descriptions.Item label="Payee ID">{tx.payeeId || '-'}</Descriptions.Item>
      </Descriptions>

      {/* Section 4: Alert */}
      <Title level={5}>Alert Information</Title>
      {tx.hasAlert && tx.alertId ? (
        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label="Alert ID">
            <Text code>{tx.alertId}</Text>
          </Descriptions.Item>
          <Descriptions.Item label="Alert Status">
            {tx.alertStatus && (
              <Tag color={statusColor[tx.alertStatus as keyof typeof statusColor] || 'default'}>
                {tx.alertStatus}
              </Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Action" span={2}>
            <Button
              type="link"
              icon={<EyeOutlined />}
              style={{ padding: 0 }}
              onClick={() => { onClose(); onViewAlert(tx.alertId!); }}
            >
              View Alert →
            </Button>
          </Descriptions.Item>
        </Descriptions>
      ) : (
        <div style={{ padding: '16px', background: '#f6f6f6', borderRadius: 4, color: '#999', textAlign: 'center' }}>
          No Alert Triggered
        </div>
      )}
    </Modal>
  );
}

export default function TransactionsList() {
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);

  const [filterStatus, setFilterStatus] = useState<string | undefined>();
  const [filterType, setFilterType] = useState<string | undefined>();
  const [filterQ, setFilterQ] = useState('');

  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    searchTransactions({
      status: filterStatus,
      type: filterType,
      q: filterQ || undefined,
      page,
      size: pageSize,
    })
      .then(data => {
        setTransactions(data.content);
        setTotal(data.totalElements);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [filterStatus, filterType, filterQ, page, pageSize]);

  useEffect(() => { load(); }, [load]);

  const openDetail = async (txId: string) => {
    try {
      const tx = await getTransaction(txId);
      setSelectedTx(tx);
      setModalOpen(true);
    } catch (e: any) {
      setError(e.message);
    }
  };

  const columns: ColumnsType<Transaction> = [
    {
      title: 'Transaction ID',
      dataIndex: 'transactionId',
      width: 180,
      ellipsis: true,
      render: (id: string) => (
        <Button
          type="link"
          style={{ padding: 0, fontSize: 12 }}
          onClick={() => openDetail(id)}
        >
          <Text code style={{ fontSize: 12 }}>{id}</Text>
        </Button>
      ),
    },
    {
      title: 'Transaction Time',
      dataIndex: 'transactionTime',
      width: 160,
      render: v => <span style={{ fontSize: 12 }}>{formatTime(v)}</span>,
    },
    {
      title: 'Account ID',
      dataIndex: 'accountId',
      width: 130,
      ellipsis: true,
      render: v => <Text type="secondary">{v}</Text>,
    },
    {
      title: 'Payee ID',
      dataIndex: 'payeeId',
      width: 130,
      ellipsis: true,
      render: v => v ? <Text type="secondary">{v}</Text> : '-',
    },
    {
      title: 'Type',
      dataIndex: 'type',
      width: 90,
      render: (v: TransactionType) => <Tag color={txTypeColor[v]}>{v}</Tag>,
    },
    {
      title: 'Amount',
      dataIndex: 'amount',
      width: 130,
      align: 'right',
      render: (v, r) => <Text strong>{formatAmount(v, r.currency)}</Text>,
    },
    { title: 'Currency', dataIndex: 'currency', width: 70 },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 110,
      render: (v: TransactionStatus) => <Tag color={txStatusColor[v]}>{v}</Tag>,
    },
    {
      title: 'Has Alert',
      dataIndex: 'hasAlert',
      width: 90,
      render: v => v ? <Tag color="red">Yes</Tag> : <Tag color="default">No</Tag>,
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>All Transactions</Title>

      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={8} lg={6}>
          <Input
            placeholder="Search transaction ID, account..."
            prefix={<SearchOutlined />}
            value={filterQ}
            onChange={e => { setFilterQ(e.target.value); setPage(0); }}
            allowClear
          />
        </Col>
        <Col xs={12} sm={6} lg={4}>
          <Select
            placeholder="Status"
            allowClear
            style={{ width: '100%' }}
            value={filterStatus}
            onChange={v => { setFilterStatus(v); setPage(0); }}
          >
            {statusOptions.map(s => (
              <Option key={s} value={s}><Tag color={txStatusColor[s]}>{s}</Tag></Option>
            ))}
          </Select>
        </Col>
        <Col xs={12} sm={6} lg={4}>
          <Select
            placeholder="Type"
            allowClear
            style={{ width: '100%' }}
            value={filterType}
            onChange={v => { setFilterType(v); setPage(0); }}
          >
            {typeOptions.map(t => (
              <Option key={t} value={t}><Tag color={txTypeColor[t]}>{t}</Tag></Option>
            ))}
          </Select>
        </Col>
        <Col>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
        </Col>
      </Row>

      {error && (
        <AntAlert
          type="error"
          message={error}
          action={<Button size="small" onClick={load}>Retry</Button>}
          style={{ marginBottom: 16 }}
          closable
        />
      )}

      <Table<Transaction>
        columns={columns}
        dataSource={transactions}
        rowKey="transactionId"
        loading={loading}
        scroll={{ x: 1200 }}
        size="middle"
        onRow={record => ({ onClick: () => openDetail(record.transactionId), style: { cursor: 'pointer' } })}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showTotal: t => `${t} transactions`,
          onChange: p => setPage(p - 1),
          showSizeChanger: false,
        }}
      />

      <TransactionDetailModal
        tx={selectedTx}
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onViewAlert={alertId => navigate(`/alerts/${alertId}`)}
      />
    </div>
  );
}
