import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Button from '../components/Button';
import Alert from '../components/Alert';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { getMyAccounts, createAccount } from '../api/accounts';
import { useAuth } from '../context/AuthContext';
import {
  formatCurrency,
  formatAccountNumber,
  formatDateTime,
  formatAccountType,
  getAccountTypeBadgeClass,
} from '../utils/formatters';

const ACCOUNT_TYPES = [
  {
    id: 'SAVINGS',
    name: 'Savings Account',
    description: 'Everyday banking with secure liquidity and compound interest on balances.',
    icon: (
      <svg className="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
  },
  {
    id: 'CURRENT',
    name: 'Current Account',
    description: 'High-volume commercial account suited for frequent transactions and business flows.',
    icon: (
      <svg className="w-5 h-5 text-sky-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
    ),
  },
  {
    id: 'SALARY',
    name: 'Salary Account',
    description: 'Zero-balance payroll account customized for corporate disbursements and salary credits.',
    icon: (
      <svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
    ),
  },
  {
    id: 'FIXED_DEPOSIT',
    name: 'Fixed Deposit Account',
    description: 'Guaranteed term deposit offering high yield for locked long-term capital preservation.',
    icon: (
      <svg className="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
      </svg>
    ),
  },
];

export const Dashboard = () => {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Modal State for Choosing Account Type
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedAccountType, setSelectedAccountType] = useState('SAVINGS');

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

  const handleOpenCreateModal = () => {
    setSelectedAccountType('SAVINGS');
    setErrorMessage('');
    setSuccessMessage('');
    setIsModalOpen(true);
  };

  const handleConfirmCreate = async () => {
    setCreating(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const newAccount = await createAccount(selectedAccountType);
      // Close modal on success
      setIsModalOpen(false);
      // Update list dynamically without full page reload
      setAccounts((prev) => [newAccount, ...prev]);
      setSuccessMessage(
        `${formatAccountType(newAccount.accountType || selectedAccountType)} (${formatAccountNumber(newAccount.accountNumber)}) created successfully!`
      );
    } catch (err) {
      console.error('Failed to create account:', err);
      setIsModalOpen(false);
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
              onClick={handleOpenCreateModal}
              disabled={loading || creating}
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
              Create your first account to begin receiving deposits and executing instant transfers.
            </p>
            <Button
              variant="primary"
              onClick={handleOpenCreateModal}
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
                    <span
                      className={`text-[11px] font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full border ${getAccountTypeBadgeClass(
                        acc.accountType || 'SAVINGS'
                      )}`}
                    >
                      {formatAccountType(acc.accountType || 'SAVINGS')}
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

      {/* Account Type Selection Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => !creating && setIsModalOpen(false)}
        title="Choose Account Type"
        confirmText="Confirm & Create"
        cancelText="Cancel"
        onConfirm={handleConfirmCreate}
        loading={creating}
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Select the type of bank account you want to open. You may hold up to 5 accounts in total.
          </p>

          <div className="grid grid-cols-1 gap-3">
            {ACCOUNT_TYPES.map((type) => {
              const isSelected = selectedAccountType === type.id;
              return (
                <div
                  key={type.id}
                  onClick={() => setSelectedAccountType(type.id)}
                  className={`cursor-pointer p-4 rounded-xl border transition-all flex items-start space-x-3.5 ${
                    isSelected
                      ? 'border-indigo-600 bg-indigo-50/40 ring-2 ring-indigo-500/20'
                      : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50/60'
                  }`}
                >
                  <div className="mt-0.5 shrink-0 p-2 rounded-lg bg-white border border-slate-200 shadow-2xs">
                    {type.icon}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-bold text-slate-900">
                        {type.name}
                      </span>
                      <span
                        className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                          isSelected
                            ? 'border-indigo-600 bg-indigo-600'
                            : 'border-slate-300 bg-white'
                        }`}
                      >
                        {isSelected && (
                          <span className="w-1.5 h-1.5 rounded-full bg-white"></span>
                        )}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                      {type.description}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Dashboard;
