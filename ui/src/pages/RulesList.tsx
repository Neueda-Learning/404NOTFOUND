import { useEffect, useState } from 'react';
import {
  Table, Tag, Button, Typography, Alert as AntAlert, Space, Switch,
  Modal, Form, Input, Select, InputNumber, Tooltip,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { listRules, createRule, updateRule, toggleRule, deleteRule } from '../api/rules';
import type { Rule, AlertSeverity, RuleType } from '../types';
import { formatTime, severityColor, ruleTypeLabel } from '../utils/format';

const { Title } = Typography;
const { Option } = Select;

const ruleTypes: RuleType[] = ['AMOUNT_THRESHOLD', 'VELOCITY', 'NEW_PAYEE', 'DAILY_LIMIT'];
const severityTypes: AlertSeverity[] = ['HIGH', 'MEDIUM', 'LOW'];
const txTypes = ['DEBIT', 'CREDIT', 'TRANSFER', 'REFUND'];

export default function RulesList() {
  const [rules, setRules] = useState<Rule[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<Rule | null>(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    setError(null);
    listRules()
      .then(setRules)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditingRule(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (rule: Rule) => {
    setEditingRule(rule);
    form.setFieldsValue(rule);
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      if (editingRule) {
        await updateRule(editingRule.id, values);
      } else {
        await createRule(values);
      }
      setModalOpen(false);
      load();
    } catch (e: any) {
      if (e?.errorFields) return; // form validation
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (id: number) => {
    try {
      await toggleRule(id);
      load();
    } catch (e: any) { setError(e.message); }
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Delete Rule',
      content: 'Are you sure you want to delete this rule? This action cannot be undone.',
      okType: 'danger',
      onOk: async () => {
        await deleteRule(id);
        load();
      },
    });
  };

  const columns: ColumnsType<Rule> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: 'Rule Name', dataIndex: 'name', ellipsis: true },
    {
      title: 'Type',
      dataIndex: 'ruleType',
      width: 140,
      render: (v: RuleType) => <Tag>{ruleTypeLabel[v] || v}</Tag>,
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      width: 90,
      render: (v: AlertSeverity) => <Tag color={severityColor[v]}>{v}</Tag>,
    },
    {
      title: 'Parameters',
      width: 220,
      render: (_, r) => {
        if (r.ruleType === 'AMOUNT_THRESHOLD') return `Threshold: ${r.threshold} ${r.currency || ''}`;
        if (r.ruleType === 'VELOCITY') return `${r.maxTransactionCount} txns / ${r.timeWindowMinutes} min`;
        if (r.ruleType === 'DAILY_LIMIT') return `Daily Limit: ${r.dailyLimit} ${r.currency || ''}`;
        return '-';
      },
    },
    {
      title: 'Active',
      dataIndex: 'active',
      width: 80,
      render: (v: boolean, r) => (
        <Switch checked={v} size="small" onChange={() => handleToggle(r.id)} />
      ),
    },
    { title: 'Version', dataIndex: 'version', width: 70 },
    { title: 'Created', dataIndex: 'createdAt', width: 150, render: v => formatTime(v) },
    {
      title: 'Actions',
      width: 100,
      render: (_, r) => (
        <Space>
          <Tooltip title="Edit"><Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} /></Tooltip>
          <Tooltip title="Delete"><Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} /></Tooltip>
        </Space>
      ),
    },
  ];

  const watchedType = Form.useWatch('ruleType', form);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>Monitoring Rules</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>New Rule</Button>
        </Space>
      </div>

      {error && (
        <AntAlert type="error" message={error} closable style={{ marginBottom: 16 }}
          action={<Button size="small" onClick={load}>Retry</Button>} />
      )}

      <Table<Rule>
        columns={columns}
        dataSource={rules}
        rowKey="id"
        loading={loading}
        scroll={{ x: 900 }}
        size="middle"
        pagination={{ pageSize: 20, showTotal: t => `${t} rules` }}
      />

      <Modal
        open={modalOpen}
        title={editingRule ? 'Edit Rule' : 'Create Rule'}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={saving}
        width={560}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="Rule Name" rules={[{ required: true }]}>
            <Input placeholder="e.g., Large Transaction Alert" />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="ruleType" label="Rule Type" rules={[{ required: true }]}>
            <Select placeholder="Select rule type">
              {ruleTypes.map(t => (
                <Option key={t} value={t}>{ruleTypeLabel[t]}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="severity" label="Severity" rules={[{ required: true }]}>
            <Select>
              {severityTypes.map(s => (
                <Option key={s} value={s}><Tag color={severityColor[s]}>{s}</Tag></Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="transactionTypes" label="Transaction Types">
            <Select mode="multiple" placeholder="Select applicable types">
              {txTypes.map(t => <Option key={t} value={t}>{t}</Option>)}
            </Select>
          </Form.Item>

          {(watchedType === 'AMOUNT_THRESHOLD') && (
            <>
              <Form.Item name="threshold" label="Threshold Amount" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={0} step={100} />
              </Form.Item>
              <Form.Item name="currency" label="Currency">
                <Input placeholder="USD" maxLength={3} style={{ width: 120 }} />
              </Form.Item>
            </>
          )}

          {watchedType === 'VELOCITY' && (
            <>
              <Form.Item name="maxTransactionCount" label="Max Transaction Count" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={1} />
              </Form.Item>
              <Form.Item name="timeWindowMinutes" label="Time Window (minutes)" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={1} />
              </Form.Item>
            </>
          )}

          {watchedType === 'DAILY_LIMIT' && (
            <>
              <Form.Item name="dailyLimit" label="Daily Limit Amount" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={0} step={1000} />
              </Form.Item>
              <Form.Item name="currency" label="Currency">
                <Input placeholder="USD" maxLength={3} style={{ width: 120 }} />
              </Form.Item>
            </>
          )}

          <Form.Item name="active" label="Active" initialValue={true}>
            <Select>
              <Option value={true}>Active</Option>
              <Option value={false}>Inactive</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
