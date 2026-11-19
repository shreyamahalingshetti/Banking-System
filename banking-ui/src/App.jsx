import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './routes/ProtectedRoute';
import AdminRoute from './routes/AdminRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AccountDetails from './pages/AccountDetails';
import NotFound from './pages/NotFound';

// Admin section
import AdminLayout from './pages/admin/AdminLayout';
import AdminOverview from './pages/admin/AdminOverview';
import AdminCustomers from './pages/admin/AdminCustomers';
import AdminAccounts from './pages/admin/AdminAccounts';
import AdminTransactions from './pages/admin/AdminTransactions';
import AdminAuditLogs from './pages/admin/AdminAuditLogs';

export const App = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected Customer Dashboard */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />

          {/* Protected Account Details Page */}
          <Route
            path="/accounts/:accountNumber"
            element={
              <ProtectedRoute>
                <AccountDetails />
              </ProtectedRoute>
            }
          />

          {/* Protected Admin Section — nested routes under AdminLayout */}
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminLayout />
              </AdminRoute>
            }
          >
            {/* /admin → Overview */}
            <Route index element={<AdminOverview />} />
            {/* /admin/customers */}
            <Route path="customers" element={<AdminCustomers />} />
            {/* /admin/accounts */}
            <Route path="accounts" element={<AdminAccounts />} />
            {/* /admin/transactions */}
            <Route path="transactions" element={<AdminTransactions />} />
            {/* /admin/audit-logs */}
            <Route path="audit-logs" element={<AdminAuditLogs />} />
          </Route>

          {/* Root → Dashboard */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* 404 Catch-all */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
