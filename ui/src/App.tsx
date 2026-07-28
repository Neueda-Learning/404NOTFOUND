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

function AppShell() {
  const location = useLocation();
  const selectedKey =
    menuItems
      .map(i => i.key)
      .filter(k => k !== '/')
      .find(k => location.pathname.startsWith(k)) ?? '/';

  return (
    <Layout className="app-frame">
      <Sider width={236} className="app-sidebar">
        <div className="brand-block">
          <Title level={5} className="brand-title">FRAML Monitor</Title>
          <div className="brand-subtitle">Transaction Monitoring</div>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          className="app-menu"
        />
      </Sider>
      <Layout className="app-main">
        <Header className="app-header">
          <Title level={5} className="header-title">
            Transaction Monitoring &amp; Alerts Dashboard
          </Title>
          <span className="header-pill">Live Monitoring</span>
        </Header>
        <Content className="app-content">
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
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppShell />
    </BrowserRouter>
  );
}

export default App;

