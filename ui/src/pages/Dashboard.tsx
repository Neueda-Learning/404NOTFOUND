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

const StatCard: FC<{
  title: string; value: number | string; icon: ReactNode;
  color?: string; suffix?: string; precision?: number;
}> = ({ title, value, icon, color = '#1890ff', suffix, precision }) => (
  <Card styles={{ body: { padding: '20px 24px' } }}>
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <Statistic
        title={title}
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
  const navigate = useNavigate();

  const load = () => {
    setLoading(true);
    setError(null);
    getDashboardSummary()
      .then(setData)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  if (loading) return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  if (error) return <Alert type="error" message={error} action={<Button onClick={load}>Retry</Button>} />;
  if (!data) return null;

  const severityRows = Object.entries(data.severityDistribution).map(([sev, count]) => ({
    severity: sev as keyof typeof severityColor, count,
  }));

  return (
    <div className="dashboard-premium">
      <Title level={4} style={{ marginBottom: 4 }}>Dashboard Overview</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        Live risk posture with alert momentum and trigger concentration.
      </Text>

      {/* KPI Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={4}>
          <StatCard title="Open Alerts" value={data.openAlerts} icon={<AlertOutlined />} color="#ff4d4f" />
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <StatCard title="Under Investigation" value={data.underInvestigation} icon={<EyeOutlined />} color="#1890ff" />
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <StatCard title="Today's Alerts" value={data.todaysAlerts} icon={<AlertOutlined />} color="#fa8c16" />
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <StatCard title="High Risk Alerts" value={data.highRiskAlerts} icon={<AlertOutlined />} color="#cf1322" />
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <StatCard title="Today's Transactions" value={data.todaysTransactions} icon={<SwapOutlined />} color="#52c41a" />
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <StatCard title="Alert Rate" value={data.alertRate} suffix="%" precision={1} icon={<RiseOutlined />} color="#225b7d" />
        </Col>
      </Row>

      {/* Charts Row */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="Alert Trend (Last 7 Days)" style={{ height: 260 }}>
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

        <Col xs={24} lg={6}>
          <Card title="Severity Distribution" style={{ height: 260 }}>
            {severityRows.map(({ severity, count }) => (
              <div key={severity} style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                  <Tag color={severityColor[severity]}>{severity}</Tag>
                  <Text strong>{count}</Text>
                </div>
                <Progress
                  percent={data.openAlerts > 0 ? Math.round((count / (data.openAlerts + data.underInvestigation + 1)) * 100) : 0}
                  strokeColor={severityColor[severity] === 'red' ? '#ff4d4f' : severityColor[severity] === 'orange' ? '#faad14' : '#1890ff'}
                  showInfo={false}
                  size="small"
                />
              </div>
            ))}
          </Card>
        </Col>

        <Col xs={24} lg={6}>
          <Card title="Top Triggered Rules" style={{ height: 260 }}>
            <Table<TopRule>
              className="monitor-table"
              dataSource={data.topTriggeredRules.slice(0, 5)}
              rowKey="ruleName"
              size="small"
              pagination={false}
              columns={[
                { title: 'Rule', dataIndex: 'ruleName', ellipsis: true },
                { title: 'Count', dataIndex: 'triggerCount', width: 60, align: 'right' },
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
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: 32 }}>🚨</div>
                <Title level={4} style={{ margin: '8px 0 4px' }}>Risk Alerts</Title>
                <Text type="secondary">{data.openAlerts} open alerts requiring attention</Text>
              </div>
              <Button type="primary" danger icon={<ArrowRightOutlined />}>
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
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: 32 }}>💳</div>
                <Title level={4} style={{ margin: '8px 0 4px' }}>All Transactions</Title>
                <Text type="secondary">{data.todaysTransactions} transactions today</Text>
              </div>
              <Button type="primary" icon={<ArrowRightOutlined />}>
                View All Transactions
              </Button>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
