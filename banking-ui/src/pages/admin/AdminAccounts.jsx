import React, { useState, useEffect, useCallback } from 'react';
import DataTable from '../../components/DataTable';
import Pagination from '../../components/Pagination';
import Button from '../../components/Button';
import Alert from '../../components/Alert';
import Modal from '../../components/Modal';
import StatusBadge from '../../components/StatusBadge';
import { getAdminAccounts, updateAccountStatus } from '../../api/admin';
import {
  formatCurrency,
  formatAccountNumber,
  formatDateTime,
  formatAccountType,
  getAccountTypeBadgeClass,
} from '../../utils/formatters';

export const AdminAccounts = () => {
  const [accounts, setAccounts] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Global success/error alerts for status change
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Modal state for status change
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [newStatus, setNewStatus] = useState('');
  const [statusChangePending, setStatusChangePending] = useState(false);

  const fetchAccounts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAdminAccounts({
        page,
        size,
        status: statusFilter,
        type: typeFilter,
      });
      setAccounts(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load accounts:', err);
      setError(err.response?.data?.message || 'Failed to fetch account list.');
    } finally {
      setLoading(false);
    }
  }, [page, size, statusFilter, typeFilter]);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  const openStatusModal = (account) => {
    setSelectedAccount(account);
    setNewStatus(account.status === 'ACTIVE' ? 'FROZEN' : 'ACTIVE');
    setModalOpen(true);
    setErrorMessage('');
    setSuccessMessage('');
  };

  const handleStatusChange = async () => {
    if (!selectedAccount || !newStatus || statusChangePending) return;
    setStatusChangePending(true);
    try {
      await updateAccountStatus(selectedAccount.accountNumber, newStatus);
      setModalOpen(false);
      setSuccessMessage(
        `Account ${formatAccountNumber(selectedAccount.accountNumber)} status changed to ${newStatus} successfully.`
      );
      await fetchAccounts();
    } catch (err) {
      console.error('Status change failed:', err);
      setModalOpen(false);
      setErrorMessage(
        err.response?.data?.message ||
          'Failed to change account status. The operation was rejected by the server.'
      );
    } finally {
      setStatusChangePending(false);
    }
  };

  const isClosed = (account) => account.status === 'CLOSED';

  const columns = [
    {
      header: 'Account Number',
      key: 'accountNumber',
      className: 'font-mono text-sm font-bold text-slate-800 tracking-wider',
      render: (row) => formatAccountNumber(row.accountNumber),
    },
    {
      header: 'Customer Name',
      key: 'customerName',
      className: 'text-slate-700 font-medium',
    },
    {
      header: 'Type',
      key: 'accountType',
      render: (row) => (
        <span
          className={`text-[11px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full border ${getAccountTypeBadgeClass(
            row.accountType || 'SAVINGS'
          )}`}
        >
          {(row.accountType || 'SAVINGS').replace('_', ' ')}
        </span>
      ),
    },
    {
      header: 'Balance',
      key: 'balance',
      className: 'text-right font-semibold text-indigo-900',
      headerClassName: 'text-right',
      render: (row) => formatCurrency(row.balance),
    },
    {
      header: 'Status',
      key: 'status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      header: 'Created',
      key: 'createdAt',
      className: 'text-xs text-slate-500 font-medium',
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      header: 'Actions',
      key: '_actions',
      render: (row) =>
        isClosed(row) ? (
          <span className="text-xs text-slate-400 italic">Permanently closed</span>
        ) : (
          <Button
            variant="secondary"
            onClick={() => openStatusModal(row)}
            className="text-xs px-2.5 py-1"
          >
            Change Status
          </Button>
        ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Account Management
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Manage account types, statuses, balances, and lifecycle across all customers
          </p>
        </div>

        <Button
          variant="secondary"
          onClick={fetchAccounts}
          disabled={loading}
          className="text-xs px-3 py-1.5 self-start sm:self-auto"
        >
          Refresh
        </Button>
      </div>

      {/* Filter Controls Bar */}
      <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-2xs space-y-3">
        {/* Status Filter */}
        <div className="flex flex-wrap items-center gap-2 sm:gap-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 min-w-[70px]">
            Status:
          </span>
          {['ALL', 'ACTIVE', 'FROZEN', 'CLOSED'].map((s) => (
            <button
              key={s}
              onClick={() => {
                setStatusFilter(s);
                setPage(0);
              }}
              className={`px-3 py-1 rounded-full text-xs font-semibold border transition-colors ${
                statusFilter === s
                  ? 'bg-indigo-600 text-white border-indigo-600'
                  : 'bg-white text-slate-600 border-slate-300 hover:border-indigo-400 hover:text-indigo-600'
              }`}
            >
              {s === 'ALL' ? 'All Statuses' : s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        {/* Account Type Filter */}
        <div className="flex flex-wrap items-center gap-2 sm:gap-3 pt-2 border-t border-slate-100">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 min-w-[70px]">
            Type:
          </span>
          {['ALL', 'SAVINGS', 'CURRENT', 'SALARY', 'FIXED_DEPOSIT'].map((t) => (
            <button
              key={t}
              onClick={() => {
                setTypeFilter(t);
                setPage(0);
              }}
              className={`px-3 py-1 rounded-full text-xs font-semibold border transition-colors ${
                typeFilter === t
                  ? 'bg-indigo-600 text-white border-indigo-600'
                  : 'bg-white text-slate-600 border-slate-300 hover:border-indigo-400 hover:text-indigo-600'
              }`}
            >
              {t === 'ALL' ? 'All Types' : t.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Alerts */}
      {successMessage && (
        <Alert type="success" message={successMessage} onClose={() => setSuccessMessage('')} />
      )}
      {errorMessage && (
        <Alert type="error" message={errorMessage} onClose={() => setErrorMessage('')} />
      )}

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <DataTable
          columns={columns}
          data={accounts}
          keyField="accountNumber"
          loading={loading}
          error={error}
          onRetry={fetchAccounts}
          emptyMessage="No accounts found"
          emptySubMessage="No accounts match the current status and type filters."
        />
        <Pagination
          page={page}
          size={size}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
          onSizeChange={(s) => {
            setSize(s);
            setPage(0);
          }}
          pageSizeOptions={[10, 20, 50]}
        />
      </div>

      {/* Status Change Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => !statusChangePending && setModalOpen(false)}
        title="Change Account Status"
        confirmText="Confirm Change"
        cancelText="Cancel"
        onConfirm={handleStatusChange}
        loading={statusChangePending}
        confirmVariant={newStatus === 'CLOSED' ? 'danger' : 'primary'}
      >
        {selectedAccount && (
          <div className="space-y-4">
            <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 text-sm space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">Account Number:</span>
                <span className="font-mono font-bold text-slate-800">
                  {formatAccountNumber(selectedAccount.accountNumber)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Account Type:</span>
                <span
                  className={`text-[11px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full border ${getAccountTypeBadgeClass(
                    selectedAccount.accountType || 'SAVINGS'
                  )}`}
                >
                  {(selectedAccount.accountType || 'SAVINGS').replace('_', ' ')}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Customer:</span>
                <span className="font-semibold text-slate-800">{selectedAccount.customerName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Current Status:</span>
                <StatusBadge status={selectedAccount.status} />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                New Status
              </label>
              <select
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value)}
                className="w-full px-3 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-200 focus:border-indigo-500"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="FROZEN">FROZEN</option>
                <option value="CLOSED">CLOSED</option>
              </select>
            </div>

            {newStatus === 'CLOSED' && (
              <div className="bg-rose-50 border border-rose-200 rounded-xl p-3 text-xs text-rose-800 font-medium flex items-start space-x-2">
                <svg className="w-4 h-4 text-rose-500 mt-0.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                <span>
                  <strong>Warning:</strong> CLOSED is permanent and irreversible. Once closed, this account cannot be reactivated or used for any transactions.
                </span>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default AdminAccounts;
