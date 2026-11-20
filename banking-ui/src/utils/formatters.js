/**
 * Format currency in Indian Rupees (INR) using standard Indian numbering system (e.g. ₹1,25,000.00).
 * Never perform arithmetic on money in the frontend; display exact backend values.
 */
export const formatCurrency = (amount) => {
  if (amount === null || amount === undefined || isNaN(Number(amount))) {
    return '—';
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(amount));
};

/**
 * Format a 12-digit account number as "XXXX XXXX XXXX" for clean readability.
 */
export const formatAccountNumber = (accNum) => {
  if (!accNum) return '•••• •••• ••••';
  return String(accNum).replace(/(\d{4})(?=\d)/g, '$1 ');
};

/**
 * Format a date-time string in Indian locale.
 */
export const formatDateTime = (dateStr) => {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return new Intl.DateTimeFormat('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(d);
  } catch {
    return String(dateStr);
  }
};

/**
 * Format account type enum string to user-friendly label (e.g. "SAVINGS ACCOUNT", "CURRENT ACCOUNT")
 */
export const formatAccountType = (type) => {
  switch (type) {
    case 'SAVINGS':
      return 'SAVINGS ACCOUNT';
    case 'CURRENT':
      return 'CURRENT ACCOUNT';
    case 'SALARY':
      return 'SALARY ACCOUNT';
    case 'FIXED_DEPOSIT':
      return 'FIXED DEPOSIT ACCOUNT';
    default:
      return type ? `${type.replace('_', ' ')} ACCOUNT` : 'SAVINGS ACCOUNT';
  }
};

/**
 * Subtle distinct badge styling per account type
 */
export const getAccountTypeBadgeClass = (type) => {
  switch (type) {
    case 'SAVINGS':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    case 'CURRENT':
      return 'bg-sky-50 text-sky-700 border-sky-200';
    case 'SALARY':
      return 'bg-purple-50 text-purple-700 border-purple-200';
    case 'FIXED_DEPOSIT':
      return 'bg-amber-50 text-amber-700 border-amber-200';
    default:
      return 'bg-slate-100 text-slate-700 border-slate-200';
  }
};
