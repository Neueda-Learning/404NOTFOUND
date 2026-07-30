import { useCallback, useEffect, useState } from 'react';
import {
  Table, Tag, Button, Input, Select, Row, Col,
  Typography, Alert as AntAlert, Modal,
  Form, InputNumber, DatePicker, message,
} from 'antd';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { searchTransactions, createTransaction } from '../api/transactions';
import type { Transaction, TransactionStatus, TransactionType } from '../types';
import {
  formatTime, txStatusColor, txTypeColor, formatAmount,
} from '../utils/format';

const { Title, Text } = Typography;
const { Option } = Select;

const statusOptions: TransactionStatus[] = ['PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REVERSED'];
const typeOptions: TransactionType[] = ['DEBIT', 'CREDIT', 'TRANSFER', 'REFUND'];
const currencyOptions = ['USD', 'EUR', 'GBP', 'CNY'];

interface CreateTransactionFormValues {
  transactionId?: string;
  accountId: string;
  payeeId?: string;
  payeeName?: string;
  type: TransactionType;
  amount: number;
  currency: string;
  status: TransactionStatus;
  transactionTime: dayjs.Dayjs;
  paymentChannel?: string;
  country?: string;
  description?: string;
}

function CreateTransactionModal({
  open, onClose, onCreated,
}: {
  open: boolean; onClose: () => void; onCreated: () => void;
}) {
  const [form] = Form.useForm<CreateTransactionFormValues>();
  const [submitting, setSubmitting] = useState(false);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const transactionId = values.transactionId?.trim() || `TXN-${Date.now()}`;
      await createTransaction({
        transactionId,
        accountId: values.accountId,
        payeeId: values.payeeId,
        payeeName: values.payeeName,
        type: values.type,
        amount: values.amount,
        currency: values.currency,
        status: values.status,
        transactionTime: values.transactionTime.toISOString(),
        paymentChannel: values.paymentChannel,
        country: values.country,
        description: values.description,
      });
      message.success(`Transaction ${transactionId} created`);
      form.resetFields();
      onCreated();
    } catch (e: any) {
      if (e?.errorFields) return; // validation error, keep modal open
      message.error(e?.response?.data?.message || e?.message || 'Failed to create transaction');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      open={open}
      title="Create Transaction"
      onCancel={handleCancel}
      onOk={handleOk}
      confirmLoading={submitting}
      okText="Create"
      width={640}
      destroyOnClose
    >
      <Form<CreateTransactionFormValues>
        form={form}
        layout="vertical"
        initialValues={{
          currency: 'USD',
          status: 'COMPLETED',
          type: 'DEBIT',
          transactionTime: dayjs(),
        }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="Transaction ID" name="transactionId" tooltip="Leave blank to auto-generate">
              <Input placeholder="Auto-generated if left blank" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="Account ID" name="accountId" rules={[{ required: true, message: 'Account ID is required' }]}>
              <Input placeholder="ACC-001" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="Payee ID" name="payeeId">
              <Input placeholder="PAYEE-001" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="Payee Name" name="payeeName">
              <Input placeholder="Payee display name" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item label="Type" name="type" rules={[{ required: true }]}>
              <Select>
                {typeOptions.map(t => <Option key={t} value={t}>{t}</Option>)}
              </Select>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item label="Amount" name="amount" rules={[{ required: true, message: 'Amount is required' }]}>
              <InputNumber<number> style={{ width: '100%' }} min={0.01} step={0.01} precision={2} placeholder="0.00" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item label="Currency" name="currency" rules={[{ required: true }]}>
              <Select>
                {currencyOptions.map(c => <Option key={c} value={c}>{c}</Option>)}
              </Select>
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="Status" name="status" rules={[{ required: true }]}>
              <Select>
                {statusOptions.map(s => <Option key={s} value={s}>{s}</Option>)}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="Transaction Time" name="transactionTime" rules={[{ required: true, message: 'Transaction time is required' }]}>
              <DatePicker showTime style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="Payment Channel" name="paymentChannel">
              <Input placeholder="WIRE / ONLINE / POS" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="Country" name="country">
              <Input placeholder="US" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="Description" name="description">
          <Input.TextArea rows={2} placeholder="Optional description" />
        </Form.Item>
      </Form>
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

  const [createModalOpen, setCreateModalOpen] = useState(false);

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

  const columns: ColumnsType<Transaction> = [
    {
      title: 'Transaction ID',
      dataIndex: 'transactionId',
      width: 180,
      ellipsis: true,
      render: (id: string) => (
        <Link
          to={`/transactions/${id}`}
          onClick={e => e.stopPropagation()}
        >
          <Text code style={{ fontSize: 15 }}>{id}</Text>
        </Link>
      ),
    },
    {
      title: 'Transaction Time',
      dataIndex: 'transactionTime',
      width: 160,
      render: v => <span style={{ fontSize: 16 }}>{formatTime(v)}</span>,
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
    {
      title: 'Status',
      dataIndex: 'status',
      width: 110,
      render: (v: TransactionStatus) => <Tag color={txStatusColor[v]}>{v}</Tag>,
    },
    {
      title: 'Alerts',
      dataIndex: 'alertCount',
      width: 110,
      render: (count: number | undefined, record: Transaction) => {
        const n = count ?? 0;
        if (n === 0) {
          return <span style={{ color: '#999' }}>No alerts</span>;
        }
        if (n === 1) {
          return (
            <Link
              to={`/alerts/${record.alertId}`}
              onClick={e => e.stopPropagation()}
              style={{ color: '#cf1322' }}
            >
              1 alert
            </Link>
          );
        }
        return (
          <Link
            to={`/transactions/${record.transactionId}`}
            onClick={e => e.stopPropagation()}
            style={{ color: '#cf1322' }}
          >
            {n} alerts
          </Link>
        );
      },
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
        <Col flex="auto" style={{ textAlign: 'right' }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>
            Create Transaction
          </Button>
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
        className="monitor-table"
        columns={columns}
        dataSource={transactions}
        rowKey="transactionId"
        loading={loading}
        scroll={{ x: 1200 }}
        size="middle"
        onRow={record => ({ onClick: () => navigate(`/transactions/${record.transactionId}`), style: { cursor: 'pointer' } })}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showTotal: t => `${t} transactions`,
          onChange: p => setPage(p - 1),
          showSizeChanger: false,
        }}
      />

      <CreateTransactionModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onCreated={() => { setCreateModalOpen(false); setPage(0); load(); }}
      />
    </div>
  );
}
