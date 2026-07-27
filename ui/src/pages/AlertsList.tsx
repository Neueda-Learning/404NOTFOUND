import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Table, Tag, Button, Input, Select, Space, Row, Col,
  Tooltip, Typography, Alert as AntAlert, Drawer, Spin,
  Descriptions, Timeline, message, Badge,
} from 'antd';
import {
  SearchOutlined, ReloadOutlined, CopyOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { searchAlerts } from '../api/alerts';
import { getAlert } from '../api/alerts';
import type { AlertListItem, AlertDetail, AlertStatus, AlertSeverity } from '../types';
import {
  formatTime, severityColor, statusColor, formatAmount, resolutionCodeLabels,
} from '../utils/format';

const { Title, Text } = Typography;
const { Option } = Select;

const statusOptions: AlertStatus[] = ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED'];
const severityOptions: AlertSeverity[] = ['HIGH', 'MEDIUM', 'LOW'];

export default function AlertsList() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [alerts, setAlerts] = useState<AlertListItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(Number(searchParams.get('page') || 0));
  const [pageSize] = useState(20);

  const [filterStatus, setFilterStatus] = useState<string | undefined>(searchParams.get('status') || undefined);
  const [filterSeverity, setFilterSeverity] = useState<string | undefined>(searchParams.get('severity') || undefined);
  const [filterQ, setFilterQ] = useState(searchParams.get('q') || '');

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [drawerAlert, setDrawerAlert] = useState<AlertDetail | null>(null);
  const [drawerError, setDrawerError] = useState<string | null>(null);
  const drawerAlertId = useRef<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    searchAlerts({
      status: filterStatus,
      severity: filterSeverity,
      q: filterQ || undefined,
      page,
      size: pageSize,
    })
      .then(data => {
        setAlerts(data.content);
        setTotal(data.totalElements);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [filterStatus, filterSeverity, filterQ, page, pageSize]);

  useEffect(() => { load(); }, [load]);

  const openDrawer = (alertId: string) => {
    drawerAlertId.current = alertId;
    setDrawerOpen(true);
    setDrawerLoading(true);
    setDrawerError(null);
    setDrawerAlert(null);
    getAlert(alertId)
      .then(setDrawerAlert)
      .catch(e => setDrawerError(e.message))
      .finally(() => setDrawerLoading(false));
  };

  const columns: ColumnsType<AlertListItem> = [
    {
      title: 'Alert ID',
      dataIndex: 'alertId',
      width: 160,
      render: (id: string) => (
        <Space>
          <Text code style={{ fontSize: 12 }}>{id}</Text>
          <Tooltip title="Copy">
            <CopyOutlined
              style={{ cursor: 'pointer', color: '#1890ff' }}
              onClick={e => { e.stopPropagation(); navigator.clipboard.writeText(id); message.success('Copied'); }}
            />
          </Tooltip>
        </Space>
      ),
    },
    {
      title: 'Created At',
      dataIndex: 'createdAt',
      width: 160,
      render: (v: string) => (
        <Tooltip title={v}>
          <span style={{ fontSize: 12 }}>{formatTime(v)}</span>
        </Tooltip>
      ),
    },
    { title: 'Account ID', dataIndex: 'accountId', width: 130, ellipsis: true },
    { title: 'Payee ID', dataIndex: 'primaryPayeeId', width: 120, ellipsis: true, render: v => v || '-' },
    { title: 'Transaction ID', dataIndex: 'primaryTransactionId', width: 140, ellipsis: true, render: v => v || '-' },
    {
      title: 'Amount',
      dataIndex: 'totalAmount',
      width: 130,
      align: 'right',
      render: (v, r) => <Text strong>{formatAmount(v, r.currency)}</Text>,
    },
    {
      title: 'Rule',
      dataIndex: 'ruleName',
      width: 160,
      ellipsis: true,
      render: (name, r) => (
        <Tooltip title={r.ruleType}>
          <Tag>{name || '-'}</Tag>
        </Tooltip>
      ),
    },
    {
      title: 'Severity',
      dataIndex: 'severity',
      width: 90,
      render: (v: AlertSeverity) => <Tag color={severityColor[v]}>{v}</Tag>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 130,
      render: (v: AlertStatus) => <Badge status={v === 'OPEN' ? 'error' : v === 'CLOSED' ? 'success' : 'processing'} text={<Tag color={statusColor[v]}>{v}</Tag>} />,
    },
    {
      title: 'Resolution',
      dataIndex: 'resolutionCode',
      width: 120,
      render: (v, r) => r.status === 'CLOSED' && v ? <Tag>{resolutionCodeLabels[v] || v}</Tag> : <Text type="secondary">-</Text>,
    },
    {
      title: 'Action',
      width: 80,
      render: (_, r) => (
        <Button size="small" type="link" onClick={e => { e.stopPropagation(); navigate(`/alerts/${r.alertId}`); }}>
          Detail
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>Risk Alerts</Title>

      {/* Filters */}
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={8} lg={6}>
          <Input
            placeholder="Search alert ID, account..."
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
              <Option key={s} value={s}><Tag color={statusColor[s]}>{s}</Tag></Option>
            ))}
          </Select>
        </Col>
        <Col xs={12} sm={6} lg={4}>
          <Select
            placeholder="Severity"
            allowClear
            style={{ width: '100%' }}
            value={filterSeverity}
            onChange={v => { setFilterSeverity(v); setPage(0); }}
          >
            {severityOptions.map(s => (
              <Option key={s} value={s}><Tag color={severityColor[s]}>{s}</Tag></Option>
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

      <Table<AlertListItem>
        columns={columns}
        dataSource={alerts}
        rowKey="alertId"
        loading={loading}
        scroll={{ x: 1400 }}
        size="middle"
        onRow={record => ({ onClick: () => openDrawer(record.alertId), style: { cursor: 'pointer' } })}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showTotal: (t) => `${t} alerts`,
          onChange: (p) => setPage(p - 1),
          showSizeChanger: false,
        }}
      />

      {/* Alert Detail Drawer */}
      <Drawer
        title={drawerAlert ? `Alert: ${drawerAlert.alertId}` : 'Alert Detail'}
        width={520}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        extra={
          drawerAlert && (
            <Button type="primary" onClick={() => navigate(`/alerts/${drawerAlert.alertId}`)}>
              View Full Detail
            </Button>
          )
        }
      >
        {drawerLoading && <div style={{ textAlign: 'center', paddingTop: 60 }}><Spin /></div>}
        {drawerError && (
          <AntAlert
            type="error"
            message={drawerError}
            action={<Button size="small" onClick={() => drawerAlertId.current && openDrawer(drawerAlertId.current)}>Retry</Button>}
          />
        )}
        {drawerAlert && !drawerLoading && (
          <div>
            <Space style={{ marginBottom: 16 }}>
              <Tag color={severityColor[drawerAlert.severity]}>{drawerAlert.severity}</Tag>
              <Tag color={statusColor[drawerAlert.status]}>{drawerAlert.status}</Tag>
              <Text type="secondary">Risk Score: <Text strong>{drawerAlert.riskScore}</Text></Text>
            </Space>

            <Descriptions column={1} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Title">{drawerAlert.title}</Descriptions.Item>
              <Descriptions.Item label="Account ID">{drawerAlert.accountId}</Descriptions.Item>
              <Descriptions.Item label="Rule">{drawerAlert.ruleName}</Descriptions.Item>
              <Descriptions.Item label="Trigger Reason">{drawerAlert.triggerReason || '-'}</Descriptions.Item>
              <Descriptions.Item label="Amount">{formatAmount(drawerAlert.totalAmount, drawerAlert.currency)}</Descriptions.Item>
              <Descriptions.Item label="Created">{formatTime(drawerAlert.createdAt)}</Descriptions.Item>
            </Descriptions>

            {drawerAlert.statusHistory && drawerAlert.statusHistory.length > 0 && (
              <>
                <Title level={5}>Status History</Title>
                <Timeline
                  items={drawerAlert.statusHistory.map(h => ({
                    color: h.toStatus === 'CLOSED' ? 'green' : h.toStatus === 'DISMISSED' ? 'gray' : 'blue',
                    children: (
                      <div>
                        <Text strong>{h.actionType}</Text>
                        <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                          {formatTime(h.changedAt)}
                        </Text>
                        <div><Tag color={statusColor[h.toStatus]}>{h.toStatus}</Tag></div>
                        {h.comment && <Text type="secondary">{h.comment}</Text>}
                      </div>
                    ),
                  }))}
                />
              </>
            )}
          </div>
        )}
      </Drawer>
    </div>
  );
}
