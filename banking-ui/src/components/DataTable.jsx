import React from 'react';
import Spinner from './Spinner';
import Alert from './Alert';

export const DataTable = ({
  columns = [],
  data = [],
  keyField = 'id',
  loading = false,
  error = null,
  onRetry,
  emptyMessage = 'No records found',
  emptySubMessage = 'There are no items matching the current selection.',
  className = '',
}) => {
  return (
    <div className={`bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden ${className}`}>
      {/* Table Container with Horizontal Scroll on Mobile */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-left text-xs sm:text-sm">
          <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[11px]">
            <tr>
              {columns.map((col, idx) => (
                <th
                  key={col.key || idx}
                  scope="col"
                  className={`px-4 sm:px-6 py-3 whitespace-nowrap ${col.headerClassName || ''}`}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-100 bg-white">
            {loading ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-16 text-center">
                  <Spinner size="md" className="text-indigo-600 mb-2" />
                  <p className="text-xs text-slate-500">Loading data from enterprise server...</p>
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-8">
                  <div className="max-w-md mx-auto text-center">
                    <Alert type="error" message={error} className="mb-3 text-left" />
                    {onRetry && (
                      <button
                        onClick={onRetry}
                        className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold underline"
                      >
                        Try Again
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-6 py-16 text-center">
                  <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-3">
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <h4 className="text-sm font-semibold text-slate-800 mb-1">{emptyMessage}</h4>
                  <p className="text-xs text-slate-500 max-w-sm mx-auto">{emptySubMessage}</p>
                </td>
              </tr>
            ) : (
              data.map((row, rowIdx) => {
                const rowKey = row[keyField] !== undefined ? row[keyField] : rowIdx;
                return (
                  <tr key={rowKey} className="hover:bg-slate-50/80 transition-colors">
                    {columns.map((col, colIdx) => (
                      <td
                        key={col.key || colIdx}
                        className={`px-4 sm:px-6 py-3.5 whitespace-nowrap text-slate-700 ${col.className || ''}`}
                      >
                        {col.render ? col.render(row, rowIdx) : row[col.key]}
                      </td>
                    ))}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default DataTable;
