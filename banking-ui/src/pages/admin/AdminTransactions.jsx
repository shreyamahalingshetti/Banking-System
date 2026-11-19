import React, { useState, useEffect, useCallback, useRef } from 'react';
import DataTable from '../../components/DataTable';
import Pagination from '../../components/Pagination';
import Button from '../../components/Button';
import StatusBadge from '../../components/StatusBadge';
import { getAdminTransactions } from '../../api/admin';
import { formatCurrency, formatDateTime } from '../../utils/formatters';

const TYPE_OPTIONS = ['ALL', 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER'];
const STATUS_OPTIONS = ['ALL', 'SUCCESS', 'FAILED'];

export const AdminTransactions = () => {
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filter state (uncommitted while typing)
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [accountNumberFilter, setAccountNumberFilter] = useState('');

  // Applied filters (sent to backend)
  const [appliedFilters, setAppliedFilters] = useState({
    type: 'ALL',
    status: 'ALL',
    accountNumber: '',
  });

  const isFiltered =
    appliedFilters.type !== 'ALL' ||
    appliedFilters.status !== 'ALL' ||
    appliedFilters.accountNumber.trim() !== '';

  const fetchTransactions = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAdminTransactions({
        page,
        size,
        type: appliedFilters.type,
        status: appliedFilters.status,
        accountNumber: appliedFilters.accountNumber,
      });
      setTransactions(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load transactions:', err);
      setError(err.response?.data?.message || 'Failed to fetch transactions.');
    } finally {
      setLoading(false);
    }
  }, [page, size, appliedFilters]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  const applyFilters = () => {
    setAppliedFilters({ type: typeFilter, status: statusFilter, accountNumber: accountNumberFilter });
    setPage(0);
  };

  const clearFilters = () => {
    setTypeFilter('ALL');
    setStatusFilter('ALL');
    setAccountNumberFilter('');
    setAppliedFilters({ type: 'ALL', status: 'ALL', accountNumber: '' });
    setPage(0);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') applyFilters();
  };

  const shortenId = (id) => {
    if (!id) return '—';
    if (id.length <= 12) return id;
    return `${id.slice(0, 8)}…${id.slice(-4)}`;
  };

  const getTypeBadge = (type) => {
    const cfg = {
      DEPOSIT: 'bg-sky-100 text-sky-800 border-sky-200',
      WITHDRAWAL: 'bg-amber-100 text-amber-800 border-amber-200',
      TRANSFER: 'bg-purple-100 text-purple-800 border-purple-200',
    };
    return (
      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold border ${cfg[type] || 'bg-slate-100 text-slate-700 border-slate-200'}`}>
        {type}
      </span>
    );
  };

  const getStatusBadge = (status) => {
    if (status === 'SUCCESS') {
      return (
        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5" />Success
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">
        <span className="w-1.5 h-1.5 rounded-full bg-rose-500 mr-1.5" />Failed
      </span>
    );
  };

  const columns = [
    {
      header: 'Date & Time',
      key: 'createdAt',
      className: 'text-xs text-slate-600 font-medium whitespace-nowrap',
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      header: 'Transaction ID',
      key: 'transactionId',
      render: (row) => (
        <span
          title={row.transactionId}
          className="font-mono text-xs text-indigo-600 cursor-help hover:underline decoration-dotted"
        >
          {shortenId(row.transactionId)}
        </span>
      ),
    },
    {
      header: 'Account Number',
      key: 'accountNumber',
      className: 'font-mono text-xs text-slate-700',
    },
    {
      header: 'Type',
      key: 'type',
      render: (row) => getTypeBadge(row.type),
    },
    {
      header: 'Amount',
      key: 'amount',
      className: 'text-right font-semibold text-slate-900',
      headerClassName: 'text-right',
      render: (row) => formatCurrency(row.amount),
    },
    {
      header: 'Balance After',
      key: 'balanceAfter',
      className: 'text-right text-slate-600',
      headerClassName: 'text-right',
      render: (row) =>
        row.balanceAfter != null ? formatCurrency(row.balanceAfter) : '—',
    },
    {
      header: 'Status',
      key: 'status',
      render: (row) => getStatusBadge(row.status),
    },
    {
      header: 'Failure Reason',
      key: 'failureReason',
      className: 'max-w-xs',
      render: (row) =>
        row.failureReason ? (
          <span className="text-xs text-rose-700 bg-rose-50 border border-rose-100 rounded px-2 py-0.5">
            {row.failureReason}
          </span>
        ) : (
          <span className="text-slate-400 text-xs">—</span>
        ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Transaction Ledger
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Complete audit trail of all deposits, withdrawals, and transfers system-wide
          </p>
        </div>
        <Button variant="secondary" onClick={fetchTransactions} disabled={loading} className="text-xs px-3 py-1.5 self-start sm:self-auto">
          Refresh
        </Button>
      </div>

      {/* Server-side Filters */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-xs space-y-3">
        <div className="flex flex-wrap items-center gap-3">
          {/* Type filter */}
          <div className="flex items-center space-x-1.5">
            <label className="text-xs font-medium text-slate-600 shrink-0">Type:</label>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="text-xs bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              {TYPE_OPTIONS.map((o) => (
                <option key={o} value={o}>{o === 'ALL' ? 'All Types' : o}</option>
              ))}
            </select>
          </div>

          {/* Status filter */}
          <div className="flex items-center space-x-1.5">
            <label className="text-xs font-medium text-slate-600 shrink-0">Status:</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="text-xs bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-200"
            >
              {STATUS_OPTIONS.map((o) => (
                <option key={o} value={o}>{o === 'ALL' ? 'All Statuses' : o}</option>
              ))}
            </select>
          </div>

          {/* Account Number filter */}
          <div className="flex items-center space-x-1.5">
            <label className="text-xs font-medium text-slate-600 shrink-0">Account:</label>
            <input
              type="text"
              placeholder="12-digit account number"
              value={accountNumberFilter}
              onChange={(e) => setAccountNumberFilter(e.target.value)}
              onKeyDown={handleKeyDown}
              className="text-xs bg-slate-50 border border-slate-300 rounded-lg px-2.5 py-1.5 text-slate-700 w-44 font-mono focus:outline-none focus:ring-2 focus:ring-indigo-200"
            />
          </div>

          {/* Action buttons */}
          <Button variant="primary" onClick={applyFilters} className="text-xs px-3 py-1.5">
            Search
          </Button>
          {isFiltered && (
            <button onClick={clearFilters} className="text-xs text-slate-500 hover:text-slate-700 font-medium underline">
              Clear Filters
            </button>
          )}
        </div>

        {isFiltered && (
          <div className="text-xs text-indigo-600 font-medium">
            Filters active — showing filtered results
          </div>
        )}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <DataTable
          columns={columns}
          data={transactions}
          keyField="transactionId"
          loading={loading}
          error={error}
          onRetry={fetchTransactions}
          emptyMessage="No transactions found"
          emptySubMessage="No transactions match the current filter criteria."
        />
        <Pagination
          page={page}
          size={size}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
          onSizeChange={(s) => { setSize(s); setPage(0); }}
          pageSizeOptions={[10, 20, 50]}
        />
      </div>
    </div>
  );
};

export default AdminTransactions;
