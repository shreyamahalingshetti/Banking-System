import React, { useState, useEffect, useCallback } from 'react';
import DataTable from '../../components/DataTable';
import Pagination from '../../components/Pagination';
import Button from '../../components/Button';
import { getAdminCustomers } from '../../api/admin';
import { formatDateTime } from '../../utils/formatters';

export const AdminCustomers = () => {
  const [customers, setCustomers] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchCustomers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAdminCustomers({ page, size });
      setCustomers(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load customers:', err);
      setError(err.response?.data?.message || 'Failed to fetch customer list.');
    } finally {
      setLoading(false);
    }
  }, [page, size]);

  useEffect(() => {
    fetchCustomers();
  }, [fetchCustomers]);

  const columns = [
    {
      header: 'ID',
      key: 'id',
      className: 'font-mono text-xs text-slate-500 font-semibold',
    },
    {
      header: 'Full Name',
      key: 'fullName',
      className: 'font-bold text-slate-900',
    },
    {
      header: 'Email Address',
      key: 'email',
      className: 'font-mono text-xs text-indigo-600',
    },
    {
      header: 'Role',
      key: 'role',
      render: (row) => (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${
            row.role === 'ADMIN'
              ? 'bg-purple-100 text-purple-800 border border-purple-200'
              : 'bg-indigo-100 text-indigo-800 border border-indigo-200'
          }`}
        >
          {row.role}
        </span>
      ),
    },
    {
      header: 'Accounts',
      key: 'accountCount',
      className: 'text-center font-bold text-slate-800',
      headerClassName: 'text-center',
      render: (row) => (
        <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-slate-100 text-slate-800 font-semibold text-xs">
          {row.accountCount ?? 0}
        </span>
      ),
    },
    {
      header: 'Joined Date',
      key: 'createdAt',
      className: 'text-xs text-slate-500 font-medium',
      render: (row) => formatDateTime(row.createdAt),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Customer Registry
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Global directory of verified customer profiles and account affiliations
          </p>
        </div>

        <Button
          variant="secondary"
          onClick={fetchCustomers}
          disabled={loading}
          className="text-xs px-3 py-1.5 self-start sm:self-auto"
        >
          Refresh List
        </Button>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <DataTable
          columns={columns}
          data={customers}
          keyField="id"
          loading={loading}
          error={error}
          onRetry={fetchCustomers}
          emptyMessage="No customers found"
          emptySubMessage="There are currently no customer records registered in the system."
        />

        {/* Reusable Pagination Component */}
        <Pagination
          page={page}
          size={size}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={(newPage) => setPage(newPage)}
          onSizeChange={(newSize) => {
            setSize(newSize);
            setPage(0);
          }}
          pageSizeOptions={[10, 20, 50]}
        />
      </div>
    </div>
  );
};

export default AdminCustomers;
