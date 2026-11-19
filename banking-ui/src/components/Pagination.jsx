import React from 'react';
import Button from './Button';

export const Pagination = ({
  page = 0,
  size = 20,
  totalPages = 0,
  totalElements = 0,
  onPageChange,
  onSizeChange,
  pageSizeOptions = [10, 20, 50],
  className = '',
}) => {
  const startItem = totalElements === 0 ? 0 : page * size + 1;
  const endItem = Math.min((page + 1) * size, totalElements);
  const currentPageDisplay = totalPages === 0 ? 0 : page + 1;

  const handlePrev = () => {
    if (page > 0 && onPageChange) {
      onPageChange(page - 1);
    }
  };

  const handleNext = () => {
    if (page < totalPages - 1 && onPageChange) {
      onPageChange(page + 1);
    }
  };

  return (
    <div
      className={`flex flex-col sm:flex-row items-center justify-between gap-4 py-3 px-4 sm:px-6 bg-white border-t border-slate-200 text-xs sm:text-sm text-slate-600 ${className}`}
    >
      {/* Left side: Results Count & Page Size */}
      <div className="flex flex-wrap items-center gap-4">
        <div>
          Showing <span className="font-semibold text-slate-800">{startItem}</span> to{' '}
          <span className="font-semibold text-slate-800">{endItem}</span> of{' '}
          <span className="font-semibold text-slate-800">{totalElements}</span> results
        </div>

        {onSizeChange && (
          <div className="flex items-center space-x-1.5">
            <span className="text-slate-500">Rows:</span>
            <select
              value={size}
              onChange={(e) => onSizeChange(Number(e.target.value))}
              className="bg-slate-50 border border-slate-300 rounded px-2 py-1 text-xs text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {pageSizeOptions.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Right side: Page X of Y & Prev/Next */}
      <div className="flex items-center space-x-3">
        <span className="text-slate-500">
          Page <span className="font-semibold text-slate-800">{currentPageDisplay}</span> of{' '}
          <span className="font-semibold text-slate-800">{totalPages || 1}</span>
        </span>

        <div className="flex items-center space-x-1.5">
          <Button
            variant="secondary"
            onClick={handlePrev}
            disabled={page <= 0}
            className="text-xs px-2.5 py-1"
          >
            Previous
          </Button>
          <Button
            variant="secondary"
            onClick={handleNext}
            disabled={page >= totalPages - 1 || totalPages === 0}
            className="text-xs px-2.5 py-1"
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Pagination;
