import { BrowserRouter, Routes, Route, NavLink, useLocation } from 'react-router-dom';
import { Layout, Menu, Typography } from 'antd';
import {
  DashboardOutlined,
  AlertOutlined,
  SwapOutlined,
  OrderedListOutlined,
} from '@ant-design/icons';
import Dashboard from './pages/Dashboard';
import AlertsList from './pages/AlertsList';
import AlertDetail from './pages/AlertDetail';
import TransactionsList from './pages/TransactionsList';
import RulesList from './pages/RulesList';
import 'antd/dist/reset.css';
import './App.css';

const { Sider, Content, Header } = Layout;
const { Title } = Typography;

const menuItems = [
  { key: '/', icon: <DashboardOutlined />, label: <NavLink to="/">Dashboard</NavLink> },
  { key: '/alerts', icon: <AlertOutlined />, label: <NavLink to="/alerts">Risk Alerts</NavLink> },
  { key: '/transactions', icon: <SwapOutlined />, label: <NavLink to="/transactions">Transactions</NavLink> },
  { key: '/rules', icon: <OrderedListOutlined />, label: <NavLink to="/rules">Rules</NavLink> },
];

function AppSider() {
  const location = useLocation();
  const selectedKey =
    menuItems
      .map(i => i.key)
      .filter(k => k !== '/')
      .find(k => location.pathname.startsWith(k)) ?? '/';

  return (
    <Sider width={220} style={{ background: '#001529' }}>
      <div style={{ padding: '16px 20px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
        <Title level={5} style={{ color: '#fff', margin: 0 }}>🛡 FRAML Monitor</Title>
        <div style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, marginTop: 2 }}>
          Transaction Monitoring
        </div>
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[selectedKey]}
        items={menuItems}
        style={{ borderRight: 0 }}
      />
    </Sider>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Layout style={{ minHeight: '100vh' }}>
        <AppSider />
        <Layout>
          <Header style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            boxShadow: '0 1px 4px rgba(0,21,41,0.08)',
          }}>
            <Title level={5} style={{ margin: 0, color: '#001529' }}>
              Transaction Monitoring &amp; Alerts Dashboard
            </Title>
          </Header>
          <Content style={{ margin: '24px', background: '#f0f2f5' }}>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/alerts" element={<AlertsList />} />
              <Route path="/alerts/:id" element={<AlertDetail />} />
              <Route path="/transactions" element={<TransactionsList />} />
              <Route path="/rules" element={<RulesList />} />
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </BrowserRouter>
  );
}

export default App;

