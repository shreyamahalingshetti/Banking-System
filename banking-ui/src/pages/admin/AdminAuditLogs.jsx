import React, { useState, useEffect, useCallback } from 'react';
import DataTable from '../../components/DataTable';
import Pagination from '../../components/Pagination';
import Button from '../../components/Button';
import { getAdminAuditLogs } from '../../api/admin';
import { formatDateTime } from '../../utils/formatters';

const ACTION_OPTIONS = [
  'ALL',
  'REGISTER',
  'LOGIN_SUCCESS',
  'LOGIN_FAILED',
  'ACCOUNT_CREATED',
  'ACCOUNT_STATUS_CHANGED',
  'TRANSFER_FAILED',
];

const ACTION_BADGE_CFG = {
  REGISTER:                { bg: 'bg-sky-100 text-sky-800 border-sky-200' },
  LOGIN_SUCCESS:           { bg: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
  LOGIN_FAILED:            { bg: 'bg-rose-100 text-rose-800 border-rose-200' },
  ACCOUNT_CREATED:         { bg: 'bg-indigo-100 text-indigo-800 border-indigo-200' },
  ACCOUNT_STATUS_CHANGED:  { bg: 'bg-amber-100 text-amber-800 border-amber-200' },
  TRANSFER_FAILED:         { bg: 'bg-rose-100 text-rose-800 border-rose-200' },
  DEPOSIT:                 { bg: 'bg-sky-100 text-sky-800 border-sky-200' },
  WITHDRAWAL:              { bg: 'bg-amber-100 text-amber-800 border-amber-200' },
  TRANSFER_SUCCESS:        { bg: 'bg-purple-100 text-purple-800 border-purple-200' },
};

const getActionBadge = (action) => {
  const cfg = ACTION_BADGE_CFG[action] || { bg: 'bg-slate-100 text-slate-700 border-slate-200' };
  const label = action.replace(/_/g, ' ');
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border whitespace-nowrap ${cfg.bg}`}>
      {label}
    </span>
  );
};

export const AdminAuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [actionFilter, setActionFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchLogs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAdminAuditLogs({ page, size, action: actionFilter });
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
      setError(err.response?.data?.message || 'Failed to fetch audit logs.');
    } finally {
      setLoading(false);
    }
  }, [page, size, actionFilter]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const columns = [
    {
      header: 'Date & Time',
      key: 'createdAt',
      className: 'text-xs text-slate-600 font-medium whitespace-nowrap',
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      header: 'Action',
      key: 'action',
      render: (row) => getActionBadge(row.action),
    },
    {
      header: 'Performed By',
      key: 'performedBy',
      className: 'font-mono text-xs text-indigo-700 font-medium',
    },
    {
      header: 'Target Type',
      key: 'targetType',
      className: 'text-xs font-semibold text-slate-600 uppercase tracking-wide',
    },
    {
      header: 'Target ID',
      key: 'targetId',
      className: 'font-mono text-xs text-slate-600',
      render: (row) => row.targetId || '—',
    },
    {
      header: 'Details',
      key: 'details',
      className: 'text-xs text-slate-500 max-w-xs',
      render: (row) => (
        <span title={row.details} className="block truncate max-w-xs">
          {row.details || '—'}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Audit Log
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Complete security and compliance event trail — newest records first
          </p>
        </div>
        <Button variant="secondary" onClick={fetchLogs} disabled={loading} className="text-xs px-3 py-1.5 self-start sm:self-auto">
          Refresh
        </Button>
      </div>

      {/* Action Filter */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-xs">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium text-slate-600 mr-1">Filter by Action:</span>
          {ACTION_OPTIONS.map((action) => (
            <button
              key={action}
              onClick={() => { setActionFilter(action); setPage(0); }}
              className={`px-2.5 py-1 rounded-full text-[11px] font-semibold border transition-colors ${
                actionFilter === action
                  ? 'bg-indigo-600 text-white border-indigo-600'
                  : 'bg-white text-slate-600 border-slate-300 hover:border-indigo-400 hover:text-indigo-600'
              }`}
            >
              {action === 'ALL' ? 'All Actions' : action.replace(/_/g, ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <DataTable
          columns={columns}
          data={logs}
          keyField="id"
          loading={loading}
          error={error}
          onRetry={fetchLogs}
          emptyMessage="No audit log entries found"
          emptySubMessage="No events match the current action filter."
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

export default AdminAuditLogs;
