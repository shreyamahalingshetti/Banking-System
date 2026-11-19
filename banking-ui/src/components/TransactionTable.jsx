import React, { useState, useMemo } from 'react';
import Spinner from './Spinner';
import Alert from './Alert';
import { formatCurrency, formatDateTime } from '../utils/formatters';

export const TransactionTable = ({ transactions = [], loading = false, error = null, onRetry }) => {
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Filter and sort transactions (newest first)
  const filteredTransactions = useMemo(() => {
    let list = [...transactions];

    // Ensure sorted by createdAt descending
    list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    if (typeFilter !== 'ALL') {
      list = list.filter((t) => t.type === typeFilter);
    }

    if (statusFilter !== 'ALL') {
      list = list.filter((t) => t.status === statusFilter);
    }

    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      list = list.filter((t) =>
        (t.transactionId || '').toLowerCase().includes(q)
      );
    }

    return list;
  }, [transactions, typeFilter, statusFilter, searchQuery]);

  const shortenId = (id) => {
    if (!id) return '—';
    if (id.length <= 12) return id;
    return `${id.slice(0, 8)}...${id.slice(-4)}`;
  };

  const getTypeBadge = (type) => {
    switch (type) {
      case 'DEPOSIT':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-100 text-sky-800 border border-sky-200">
            Deposit
          </span>
        );
      case 'WITHDRAWAL':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
            Withdrawal
          </span>
        );
      case 'TRANSFER':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 border border-purple-200">
            Transfer
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-800 border border-slate-200">
            {type}
          </span>
        );
    }
  };

  const getStatusBadge = (status) => {
    if (status === 'SUCCESS') {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5" />
          Success
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">
        <span className="w-1.5 h-1.5 rounded-full bg-rose-500 mr-1.5" />
        Failed
      </span>
    );
  };

  const isFiltered = typeFilter !== 'ALL' || statusFilter !== 'ALL' || searchQuery.trim() !== '';

  const handleResetFilters = () => {
    setTypeFilter('ALL');
    setStatusFilter('ALL');
    setSearchQuery('');
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
      {/* Header and Controls */}
      <div className="p-4 sm:p-6 border-b border-slate-100 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h3 className="text-lg font-bold text-slate-900">Transaction History</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Complete chronological audit trail for this account
            </p>
          </div>
          <div className="text-xs text-slate-500 font-medium">
            Showing {filteredTransactions.length} of {transactions.length} record(s)
          </div>
        </div>

        {/* Filter controls */}
        <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 pt-2">
          {/* Search by transaction ID */}
          <div className="sm:col-span-5">
            <div className="relative">
              <input
                type="text"
                placeholder="Search by Transaction ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm bg-slate-50 border border-slate-300 rounded-lg focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-200 focus:border-indigo-500 transition-colors"
              />
              <svg
                className="w-4 h-4 text-slate-400 absolute left-3 top-2.5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
          </div>

          {/* Type filter */}
          <div className="sm:col-span-3">
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="w-full px-3 py-2 text-xs sm:text-sm bg-slate-50 border border-slate-300 rounded-lg focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-200 focus:border-indigo-500 transition-colors text-slate-700"
            >
              <option value="ALL">All Types</option>
              <option value="DEPOSIT">Deposit</option>
              <option value="WITHDRAWAL">Withdrawal</option>
              <option value="TRANSFER">Transfer</option>
            </select>
          </div>

          {/* Status filter */}
          <div className="sm:col-span-3">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full px-3 py-2 text-xs sm:text-sm bg-slate-50 border border-slate-300 rounded-lg focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-200 focus:border-indigo-500 transition-colors text-slate-700"
            >
              <option value="ALL">All Statuses</option>
              <option value="SUCCESS">Success</option>
              <option value="FAILED">Failed</option>
            </select>
          </div>

          {/* Clear Filters */}
          {isFiltered && (
            <div className="sm:col-span-1 flex items-center justify-start sm:justify-center">
              <button
                onClick={handleResetFilters}
                className="text-xs text-indigo-600 hover:text-indigo-800 font-medium hover:underline py-1"
                title="Reset filters"
              >
                Reset
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Body / Content */}
      {loading ? (
        <div className="p-12 text-center">
          <Spinner size="md" className="text-indigo-600 mb-2" />
          <p className="text-sm text-slate-500">Loading transaction history...</p>
        </div>
      ) : error ? (
        <div className="p-6">
          <Alert type="error" message={error} className="mb-4" />
          {onRetry && (
            <div className="text-center">
              <button
                onClick={onRetry}
                className="text-sm font-medium text-indigo-600 hover:text-indigo-800 underline"
              >
                Retry loading transactions
              </button>
            </div>
          )}
        </div>
      ) : filteredTransactions.length === 0 ? (
        <div className="p-12 text-center">
          <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-3">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <h4 className="text-sm font-semibold text-slate-800 mb-1">
            {transactions.length === 0
              ? 'No transactions yet'
              : 'No transactions match your search/filter criteria'}
          </h4>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            {transactions.length === 0
              ? 'When deposits, withdrawals, or transfers take place, they will appear here.'
              : 'Try clearing your filters or searching for another transaction ID.'}
          </p>
          {isFiltered && (
            <button
              onClick={handleResetFilters}
              className="mt-3 text-xs text-indigo-600 font-semibold hover:underline"
            >
              Reset filters
            </button>
          )}
        </div>
      ) : (
        <>
          {/* Desktop Table View (hidden on mobile) */}
          <div className="hidden md:block overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs sm:text-sm">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[11px]">
                <tr>
                  <th scope="col" className="px-6 py-3">Date & Time</th>
                  <th scope="col" className="px-6 py-3">Transaction ID</th>
                  <th scope="col" className="px-6 py-3">Type</th>
                  <th scope="col" className="px-6 py-3 text-right">Amount</th>
                  <th scope="col" className="px-6 py-3 text-right">Balance After</th>
                  <th scope="col" className="px-6 py-3">Status</th>
                  <th scope="col" className="px-6 py-3">Details / Failure Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {filteredTransactions.map((tx) => (
                  <tr key={tx.transactionId || tx.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap text-slate-600 font-medium text-xs">
                      {formatDateTime(tx.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap font-mono text-xs text-indigo-600 font-medium">
                      <span
                        title={tx.transactionId}
                        className="cursor-help hover:underline decoration-dotted"
                      >
                        {shortenId(tx.transactionId)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getTypeBadge(tx.type)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right font-semibold text-slate-900">
                      {formatCurrency(tx.amount)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-slate-600 font-medium">
                      {tx.balanceAfter !== null && tx.balanceAfter !== undefined
                        ? formatCurrency(tx.balanceAfter)
                        : '—'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getStatusBadge(tx.status)}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500 max-w-xs break-words">
                      {tx.status === 'FAILED' ? (
                        <span className="text-rose-600 font-medium bg-rose-50 px-2 py-0.5 rounded border border-rose-100 inline-block">
                          {tx.failureReason || 'Operation failed'}
                        </span>
                      ) : (
                        <span className="text-slate-400">Completed successfully</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Stacked Card View (hidden on desktop) */}
          <div className="md:hidden divide-y divide-slate-100">
            {filteredTransactions.map((tx) => (
              <div key={tx.transactionId || tx.id} className="p-4 space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-400">
                    {formatDateTime(tx.createdAt)}
                  </span>
                  {getStatusBadge(tx.status)}
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    {getTypeBadge(tx.type)}
                    <span
                      title={tx.transactionId}
                      className="font-mono text-xs text-indigo-600 cursor-help"
                    >
                      {shortenId(tx.transactionId)}
                    </span>
                  </div>
                  <div className="text-base font-bold text-slate-900">
                    {formatCurrency(tx.amount)}
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-500 pt-1">
                  <span>Balance After:</span>
                  <span className="font-semibold text-slate-700">
                    {tx.balanceAfter !== null && tx.balanceAfter !== undefined
                      ? formatCurrency(tx.balanceAfter)
                      : '—'}
                  </span>
                </div>

                {tx.status === 'FAILED' && tx.failureReason && (
                  <div className="text-xs text-rose-700 bg-rose-50 border border-rose-100 rounded-lg p-2 font-medium">
                    Reason: {tx.failureReason}
                  </div>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default TransactionTable;
