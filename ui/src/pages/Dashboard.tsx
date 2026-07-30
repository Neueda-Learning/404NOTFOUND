import { useEffect, useState } from 'react';
import type { FC, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Row, Col, Card, Statistic, Typography, Spin, Alert,
  Table, Tag, Progress, Button,
} from 'antd';
import {
  AlertOutlined, EyeOutlined, SwapOutlined, RiseOutlined,
  ArrowRightOutlined,
} from '@ant-design/icons';
import { getDashboardSummary } from '../api/dashboard';
import type { DashboardSummary as DSType, AlertTrendPoint, TopRule } from '../types';
import { severityColor } from '../utils/format';

const { Title, Text } = Typography;

const DEMO_DASHBOARD_DATA: DSType = {
  openAlerts: 21,
  underInvestigation: 8,
  todaysAlerts: 11,
  highRiskAlerts: 14,
  todaysTransactions: 312,
  alertRate: 9.3,
  alertTrend: [
    { date: '2026-07-22', high: 2, medium: 3, low: 2, total: 7 },
    { date: '2026-07-23', high: 3, medium: 2, low: 3, total: 8 },
    { date: '2026-07-24', high: 2, medium: 3, low: 3, total: 8 },
    { date: '2026-07-25', high: 4, medium: 3, low: 2, total: 9 },
    { date: '2026-07-26', high: 3, medium: 4, low: 2, total: 9 },
    { date: '2026-07-27', high: 5, medium: 3, low: 1, total: 9 },
    { date: '2026-07-28', high: 4, medium: 4, low: 2, total: 10 },
  ],
  severityDistribution: {
    HIGH: 14,
    MEDIUM: 9,
    LOW: 6,
  },
  topTriggeredRules: [
    { ruleName: 'High-value USD debit', triggerCount: 16 },
    { ruleName: 'Daily USD debit total above 50,000', triggerCount: 11 },
    { ruleName: 'More than 5 debits in 10 minutes', triggerCount: 9 },
    { ruleName: 'First completed debit to payee', triggerCount: 7 },
    { ruleName: 'Velocity + New Payee correlation', triggerCount: 5 },
  ],
};

const StatCard: FC<{
  title: string; value: number | string; icon: ReactNode;
  color?: string; suffix?: string; precision?: number;
}> = ({ title, value, icon, color = '#1890ff', suffix, precision }) => (
  <Card styles={{ body: { padding: '20px 24px' } }}>
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <Statistic
        title={<span className="kpi-title-one-line">{title}</span>}
        value={value}
        suffix={suffix}
        precision={precision}
        styles={{ content: { color, fontSize: 28, fontWeight: 700 } }}
      />
      <div style={{
        width: 52, height: 52, borderRadius: '50%',
        background: `${color}18`, display: 'flex', alignItems: 'center',
        justifyContent: 'center', fontSize: 24, color,
      }}>
        {icon}
      </div>
    </div>
  </Card>
);

const SimpleBarChart: FC<{ data: AlertTrendPoint[] }> = ({ data }) => {
  const maxVal = Math.max(...data.map(d => d.total), 1);

  const barHeight = (value: number) => `${Math.max(4, (value / maxVal) * 100)}%`;

  return (
    <div className="premium-trend-wrap">
      {data.map((d, i) => (
        <div key={i} className="premium-trend-col" style={{ animationDelay: `${i * 70}ms` }}>
          <div className="premium-trend-track">
            <div className="premium-trend-stack" style={{ height: barHeight(d.total) }}>
              <div className="premium-seg premium-seg-low" style={{ height: `${(d.low / d.total) * 100 || 0}%` }} />
              <div className="premium-seg premium-seg-medium" style={{ height: `${(d.medium / d.total) * 100 || 0}%` }} />
              <div className="premium-seg premium-seg-high" style={{ height: `${(d.high / d.total) * 100 || 0}%` }} />
            </div>
          </div>
          <Text className="premium-trend-date">
            {d.date?.slice(5)}
          </Text>
          <Text className="premium-trend-value">{d.total}</Text>
        </div>
      ))}
    </div>
  );
};

export default function Dashboard() {
  const [data, setData] = useState<DSType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [usingFallbackData, setUsingFallbackData] = useState(false);
  const navigate = useNavigate();

  const load = () => {
    setLoading(true);
    setError(null);
    setUsingFallbackData(false);
    getDashboardSummary()
      .then(setData)
      .catch(e => {
        setData(DEMO_DASHBOARD_DATA);
        setUsingFallbackData(true);
        setError(e.message);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  if (loading) return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  if (error && !data) return <Alert type="error" message={error} action={<Button onClick={load}>Retry</Button>} />;
  if (!data) return null;

  const severityRows = Object.entries(data.severityDistribution).map(([sev, count]) => ({
    severity: sev as keyof typeof severityColor, count,
  }));

  return (
    <div className="dashboard-premium">
      {usingFallbackData && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="Backend is unavailable. Showing demo dashboard data."
          action={<Button onClick={load}>Retry API</Button>}
        />
      )}
      <Title level={4} style={{ marginBottom: 4 }}>Dashboard Overview</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        Live risk posture with alert momentum and trigger concentration.
      </Text>

      <Card className="dashboard-hero-card" style={{ marginBottom: 20 }}>
        <div className="dashboard-hero-layout">
          <div>
            <Text className="dashboard-hero-eyebrow">Operational Snapshot</Text>
            <Title level={3} style={{ margin: '8px 0 8px' }}>
              {data.openAlerts + data.underInvestigation} active alert cases
            </Title>
            <Text type="secondary">
              Strongest signal today comes from high risk events and top triggered rules.
            </Text>
            <div className="dashboard-hero-chips">
              <span className="hero-chip hero-chip-danger">{data.highRiskAlerts} High risk</span>
              <span className="hero-chip hero-chip-info">{data.todaysAlerts} New today</span>
              <span className="hero-chip hero-chip-success">{data.todaysTransactions} Transactions</span>
            </div>
          </div>
        </div>
      </Card>

      {/* KPI Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={8} lg={4}>
          <StatCard title="Open Alerts" value={data.openAlerts} icon={<AlertOutlined />} color="#ff4d4f" />
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <StatCard title="Under Investigation" value={data.underInvestigation} icon={<EyeOutlined />} color="#1890ff" />
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <StatCard title="Today's Alerts" value={data.todaysAlerts} icon={<AlertOutlined />} color="#fa8c16" />
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <StatCard title="High Risk Alerts" value={data.highRiskAlerts} icon={<AlertOutlined />} color="#cf1322" />
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <StatCard title="Today's Transactions" value={data.todaysTransactions} icon={<SwapOutlined />} color="#52c41a" />
        </Col>
        <Col xs={24} sm={12} md={8} lg={4}>
          <StatCard title="Alert Rate" value={data.alertRate} suffix="%" precision={1} icon={<RiseOutlined />} color="#225b7d" />
        </Col>
      </Row>

      {/* Charts Row */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card className="dashboard-chart-card" title="Alert Trend (Last 7 Days)">
            {data.alertTrend.length > 0 ? (
              <>
                <SimpleBarChart data={data.alertTrend} />
                <div className="premium-chart-legend">
                  <span><i className="dot-high" /> High</span>
                  <span><i className="dot-medium" /> Medium</span>
                  <span><i className="dot-low" /> Low</span>
                </div>
              </>
            ) : (
              <div style={{ textAlign: 'center', color: '#999', paddingTop: 40 }}>No data available</div>
            )}
          </Card>
        </Col>

        <Col xs={24} md={12} lg={6}>
          <Card className="dashboard-side-card" title="Severity Distribution">
            {severityRows.map(({ severity, count }) => (
              <div key={severity} style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                  <Tag color={severityColor[severity]}>{severity}</Tag>
                  <Text strong>{count}</Text>
                </div>
                <Progress
                  percent={data.openAlerts > 0 ? Math.round((count / (data.openAlerts + data.underInvestigation + 1)) * 100) : 0}
                  strokeColor={severityColor[severity]}
                  showInfo={false}
                  size="small"
                />
              </div>
            ))}
          </Card>
        </Col>

        <Col xs={24} md={12} lg={6}>
          <Card className="dashboard-side-card" title="Top Triggered Rules">
            <Table<TopRule>
              className="monitor-table dashboard-rules-table"
              dataSource={data.topTriggeredRules.slice(0, 5)}
              rowKey="ruleName"
              size="small"
              pagination={false}
              tableLayout="fixed"
              columns={[
                { title: 'Rule', dataIndex: 'ruleName', ellipsis: { showTitle: true } },
                { title: 'Count', dataIndex: 'triggerCount', width: 70, align: 'right' },
              ]}
            />
          </Card>
        </Col>
      </Row>

      {/* Navigation Cards */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12}>
          <Card
            className="premium-nav-card premium-nav-alerts"
            hoverable
            onClick={() => navigate('/alerts')}
            style={{ cursor: 'pointer', borderColor: '#ff4d4f' }}
          >
            <div className="premium-nav-layout" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: 32 }}>🚨</div>
                <Title level={4} style={{ margin: '8px 0 4px' }}>Risk Alerts</Title>
                <Text type="secondary">{data.openAlerts} open alerts requiring attention</Text>
              </div>
              <Button className="premium-nav-action" type="primary" danger icon={<ArrowRightOutlined />}>
                View All Alerts
              </Button>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12}>
          <Card
            className="premium-nav-card premium-nav-transactions"
            hoverable
            onClick={() => navigate('/transactions')}
            style={{ cursor: 'pointer', borderColor: '#1890ff' }}
          >
            <div className="premium-nav-layout" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: 32 }}>💳</div>
                <Title level={4} style={{ margin: '8px 0 4px' }}>All Transactions</Title>
                <Text type="secondary">{data.todaysTransactions} transactions today</Text>
              </div>
              <Button className="premium-nav-action" type="primary" icon={<ArrowRightOutlined />}>
                View All Transactions
              </Button>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
