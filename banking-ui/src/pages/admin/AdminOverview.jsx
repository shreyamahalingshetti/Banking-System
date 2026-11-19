import React, { useState, useEffect, useCallback } from 'react';
import Button from '../../components/Button';
import Alert from '../../components/Alert';
import Spinner from '../../components/Spinner';
import { getAdminSummary } from '../../api/admin';
import { formatCurrency } from '../../utils/formatters';

export const AdminOverview = () => {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchSummary = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAdminSummary();
      setSummary(data);
    } catch (err) {
      console.error('Failed to load admin summary:', err);
      setError(
        err.response?.data?.message ||
          'Failed to load dashboard overview. Please check your network and try again.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  // Calculations for visual breakdown bars
  const totalAccounts = summary?.totalAccounts || 0;
  const activePct = totalAccounts > 0 ? ((summary.activeAccounts / totalAccounts) * 100).toFixed(1) : 0;
  const frozenPct = totalAccounts > 0 ? ((summary.frozenAccounts / totalAccounts) * 100).toFixed(1) : 0;
  const closedPct = totalAccounts > 0 ? ((summary.closedAccounts / totalAccounts) * 100).toFixed(1) : 0;

  const totalTx = summary?.totalTransactions || 0;
  const successTx = summary?.successfulTransactions || 0;
  const failedTx = summary?.failedTransactions || 0;
  const successPct = totalTx > 0 ? ((successTx / totalTx) * 100).toFixed(1) : 0;
  const failedPct = totalTx > 0 ? ((failedTx / totalTx) * 100).toFixed(1) : 0;

  return (
    <div className="space-y-6">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Executive Overview
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Real-time financial metrics, customer volume, and enterprise transaction health
          </p>
        </div>

        <Button
          variant="secondary"
          onClick={fetchSummary}
          disabled={loading}
          className="text-xs px-3 py-1.5 self-start sm:self-auto"
        >
          {loading ? (
            <>
              <Spinner size="sm" className="mr-2" /> Refreshing...
            </>
          ) : (
            <>
              <svg className="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Refresh Data
            </>
          )}
        </Button>
      </div>

      {/* Error Alert */}
      {error && (
        <Alert
          type="error"
          message={error}
          onClose={() => setError(null)}
        />
      )}

      {/* Loading Skeleton */}
      {loading && !summary ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
            <div key={i} className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs animate-pulse">
              <div className="h-4 bg-slate-200 rounded w-1/3 mb-4" />
              <div className="h-8 bg-slate-200 rounded w-2/3" />
            </div>
          ))}
        </div>
      ) : summary ? (
        <>
          {/* Top 4 Key Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {/* Total Balance */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs flex flex-col justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Total Balance Across Accounts
              </span>
              <div className="text-2xl sm:text-3xl font-black text-indigo-950 mt-2">
                {formatCurrency(summary.totalBalanceAcrossAccounts)}
              </div>
              <span className="text-[11px] text-slate-500 mt-2">
                Aggregated system liquid liability
              </span>
            </div>

            {/* Total Customers */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs flex flex-col justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Total Customers
              </span>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {summary.totalCustomers.toLocaleString('en-IN')}
              </div>
              <span className="text-[11px] text-slate-500 mt-2">
                Registered platform accounts
              </span>
            </div>

            {/* Total Accounts */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs flex flex-col justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Total Accounts
              </span>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {summary.totalAccounts.toLocaleString('en-IN')}
              </div>
              <div className="flex items-center space-x-2 text-[11px] text-slate-500 mt-2">
                <span className="text-emerald-600 font-semibold">{summary.activeAccounts} Active</span>
                <span>•</span>
                <span className="text-amber-600 font-semibold">{summary.frozenAccounts} Frozen</span>
                <span>•</span>
                <span className="text-rose-600 font-semibold">{summary.closedAccounts} Closed</span>
              </div>
            </div>

            {/* Total Transactions */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs flex flex-col justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Total Transactions
              </span>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 mt-2">
                {summary.totalTransactions.toLocaleString('en-IN')}
              </div>
              <span className="text-[11px] text-slate-500 mt-2 font-medium">
                <span className="text-indigo-600 font-bold">{summary.transactionsToday}</span> transactions today
              </span>
            </div>
          </div>

          {/* Money Volume Flow Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* Total Deposited */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Total Deposited
                </span>
                <span className="w-8 h-8 rounded-lg bg-sky-100 text-sky-700 flex items-center justify-center text-sm font-bold">
                  ↓
                </span>
              </div>
              <div className="text-2xl font-black text-sky-900">
                {formatCurrency(summary.totalDepositedAmount)}
              </div>
              <p className="text-xs text-slate-400 mt-1">Total lifetime inflow into accounts</p>
            </div>

            {/* Total Withdrawn */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Total Withdrawn
                </span>
                <span className="w-8 h-8 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center text-sm font-bold">
                  ↑
                </span>
              </div>
              <div className="text-2xl font-black text-amber-900">
                {formatCurrency(summary.totalWithdrawnAmount)}
              </div>
              <p className="text-xs text-slate-400 mt-1">Total lifetime disbursements to customers</p>
            </div>

            {/* Total Transferred */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Total Transferred
                </span>
                <span className="w-8 h-8 rounded-lg bg-purple-100 text-purple-700 flex items-center justify-center text-sm font-bold">
                  ⇄
                </span>
              </div>
              <div className="text-2xl font-black text-purple-900">
                {formatCurrency(summary.totalTransferredAmount)}
              </div>
              <p className="text-xs text-slate-400 mt-1">Inter-account double-entry volume</p>
            </div>
          </div>

          {/* Plain Tailwind Visual Progress Breakdowns */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Account Status Split */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="text-sm font-bold text-slate-800">
                  Account Status Distribution
                </h3>
                <span className="text-xs text-slate-500">
                  {totalAccounts} total
                </span>
              </div>

              {/* Stacked Progress Bar */}
              <div className="w-full h-4 bg-slate-100 rounded-full overflow-hidden flex">
                <div
                  style={{ width: `${activePct}%` }}
                  className="bg-emerald-500 transition-all duration-500"
                  title={`Active: ${activePct}%`}
                />
                <div
                  style={{ width: `${frozenPct}%` }}
                  className="bg-amber-500 transition-all duration-500"
                  title={`Frozen: ${frozenPct}%`}
                />
                <div
                  style={{ width: `${closedPct}%` }}
                  className="bg-rose-500 transition-all duration-500"
                  title={`Closed: ${closedPct}%`}
                />
              </div>

              {/* Legend */}
              <div className="grid grid-cols-3 gap-2 pt-2 text-xs">
                <div className="flex items-center space-x-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shrink-0" />
                  <div>
                    <span className="font-semibold text-slate-700 block">Active ({summary.activeAccounts})</span>
                    <span className="text-slate-400">{activePct}%</span>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-amber-500 shrink-0" />
                  <div>
                    <span className="font-semibold text-slate-700 block">Frozen ({summary.frozenAccounts})</span>
                    <span className="text-slate-400">{frozenPct}%</span>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-rose-500 shrink-0" />
                  <div>
                    <span className="font-semibold text-slate-700 block">Closed ({summary.closedAccounts})</span>
                    <span className="text-slate-400">{closedPct}%</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Transaction Health Ratio */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="text-sm font-bold text-slate-800">
                  Transaction Success Ratio
                </h3>
                <span className="text-xs text-slate-500">
                  {totalTx} total
                </span>
              </div>

              {/* Stacked Progress Bar */}
              <div className="w-full h-4 bg-slate-100 rounded-full overflow-hidden flex">
                <div
                  style={{ width: `${successPct}%` }}
                  className="bg-emerald-500 transition-all duration-500"
                  title={`Success: ${successPct}%`}
                />
                <div
                  style={{ width: `${failedPct}%` }}
                  className="bg-rose-500 transition-all duration-500"
                  title={`Failed: ${failedPct}%`}
                />
              </div>

              {/* Legend */}
              <div className="grid grid-cols-2 gap-2 pt-2 text-xs">
                <div className="flex items-center space-x-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shrink-0" />
                  <div>
                    <span className="font-semibold text-slate-700 block">
                      Success ({summary.successfulTransactions})
                    </span>
                    <span className="text-emerald-600 font-medium">{successPct}%</span>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-rose-500 shrink-0" />
                  <div>
                    <span className="font-semibold text-slate-700 block">
                      Failed ({summary.failedTransactions})
                    </span>
                    <span className="text-rose-600 font-medium">{failedPct}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
};

export default AdminOverview;
