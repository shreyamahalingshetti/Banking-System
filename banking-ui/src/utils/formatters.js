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
