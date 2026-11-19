import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Button from '../components/Button';
import Alert from '../components/Alert';
import StatusBadge from '../components/StatusBadge';
import { getMyAccounts, createAccount } from '../api/accounts';
import { useAuth } from '../context/AuthContext';
import { formatCurrency, formatAccountNumber, formatDateTime } from '../utils/formatters';

export const Dashboard = () => {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const fetchAccounts = useCallback(async () => {
    try {
      setLoading(true);
      setErrorMessage('');
      const data = await getMyAccounts();
      setAccounts(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load accounts:', err);
      if (err.response?.data?.message) {
        setErrorMessage(err.response.data.message);
      } else {
        setErrorMessage('Failed to load your accounts. Please check your connection and try again.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  const handleCreateAccount = async () => {
    setCreating(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const newAccount = await createAccount();
      // Update list without full page reload
      setAccounts((prev) => [newAccount, ...prev]);
      setSuccessMessage(`Account ${formatAccountNumber(newAccount.accountNumber)} created successfully!`);
    } catch (err) {
      console.error('Failed to create account:', err);
      if (err.response?.data?.message) {
        setErrorMessage(err.response.data.message);
      } else {
        setErrorMessage('Failed to create a new bank account. Please try again.');
      }
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome and Action Bar */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 mb-8 gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Welcome back, {user?.fullName || user?.email || 'Customer'}
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              Manage your accounts, balances, and enterprise banking operations
            </p>
          </div>

          <div>
            <Button
              variant="primary"
              onClick={handleCreateAccount}
              loading={creating}
              disabled={creating || loading}
              className="w-full sm:w-auto shadow-sm"
            >
              <svg
                className="w-4 h-4 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M12 4v16m8-8H4"
                />
              </svg>
              Create new account
            </Button>
          </div>
        </div>

        {/* Alerts */}
        {successMessage && (
          <Alert
            type="success"
            message={successMessage}
            onClose={() => setSuccessMessage('')}
            className="mb-6"
          />
        )}

        {errorMessage && (
          <Alert
            type="error"
            message={errorMessage}
            onClose={() => setErrorMessage('')}
            className="mb-6"
          />
        )}

        {/* Accounts Overview */}
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-800">
            Your Bank Accounts ({accounts.length})
          </h2>
          <Button
            variant="secondary"
            onClick={fetchAccounts}
            disabled={loading}
            className="text-xs px-2.5 py-1"
          >
            Refresh
          </Button>
        </div>

        {/* Loading State */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs animate-pulse"
              >
                <div className="h-4 bg-slate-200 rounded w-1/3 mb-4"></div>
                <div className="h-7 bg-slate-200 rounded w-2/3 mb-6"></div>
                <div className="h-4 bg-slate-200 rounded w-1/2"></div>
              </div>
            ))}
          </div>
        ) : accounts.length === 0 ? (
          /* Empty State */
          <div className="bg-white rounded-2xl border-2 border-dashed border-slate-300 p-12 text-center max-w-xl mx-auto my-8">
            <div className="mx-auto w-14 h-14 rounded-full bg-indigo-50 text-indigo-600 flex items-center justify-center mb-4">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-slate-900 mb-1">
              You don't have any accounts yet
            </h3>
            <p className="text-sm text-slate-500 mb-6">
              Create your first one to begin receiving deposits and executing instant transfers.
            </p>
            <Button
              variant="primary"
              onClick={handleCreateAccount}
              loading={creating}
              disabled={creating}
            >
              Create your first account
            </Button>
          </div>
        ) : (
          /* Accounts Grid - Clickable Cards Linking to /accounts/:accountNumber */
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {accounts.map((acc) => (
              <Link
                key={acc.id || acc.accountNumber}
                to={`/accounts/${acc.accountNumber}`}
                className="group bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md hover:border-indigo-300 transition-all duration-200 flex flex-col justify-between block focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 group-hover:text-indigo-600 transition-colors">
                      Savings Account
                    </span>
                    <StatusBadge status={acc.status} />
                  </div>

                  <div className="mb-4">
                    <div className="text-xs text-slate-500 mb-1">Account Number</div>
                    <div className="font-mono text-base font-bold text-slate-800 tracking-wider group-hover:text-indigo-600 transition-colors">
                      {formatAccountNumber(acc.accountNumber)}
                    </div>
                  </div>

                  <div className="mb-4">
                    <div className="text-xs text-slate-500 mb-1">Available Balance</div>
                    <div className="text-2xl font-black text-indigo-900">
                      {formatCurrency(acc.balance)}
                    </div>
                  </div>
                </div>

                <div className="border-t border-slate-100 pt-4 mt-2 flex items-center justify-between text-xs text-slate-400">
                  <span>Opened on</span>
                  <span className="font-medium text-slate-600">
                    {formatDateTime(acc.createdAt)}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};

export default Dashboard;
