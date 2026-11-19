import React from 'react';

export const StatusBadge = ({ status = 'ACTIVE', className = '' }) => {
  const normalizedStatus = (status || '').toUpperCase();

  const statusConfig = {
    ACTIVE: {
      bg: 'bg-emerald-100 text-emerald-800 border-emerald-200',
      dot: 'bg-emerald-500',
      label: 'Active',
    },
    FROZEN: {
      bg: 'bg-amber-100 text-amber-800 border-amber-200',
      dot: 'bg-amber-500',
      label: 'Frozen',
    },
    CLOSED: {
      bg: 'bg-rose-100 text-rose-800 border-rose-200',
      dot: 'bg-rose-500',
      label: 'Closed',
    },
  };

  const config = statusConfig[normalizedStatus] || {
    bg: 'bg-slate-100 text-slate-800 border-slate-200',
    dot: 'bg-slate-400',
    label: normalizedStatus,
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${config.bg} ${className}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full mr-1.5 ${config.dot}`} />
      {config.label}
    </span>
  );
};

export default StatusBadge;
