import api from './axios';

/**
 * Fetch executive dashboard summary metrics.
 */
export const getAdminSummary = async () => {
  const response = await api.get('/api/admin/summary');
  return response.data;
};

/**
 * List customers with pagination.
 */
export const getAdminCustomers = async ({ page = 0, size = 20 } = {}) => {
  const response = await api.get('/api/admin/customers', {
    params: { page, size },
  });
  return response.data;
};

/**
 * List accounts with pagination and optional status & type filters.
 */
export const getAdminAccounts = async ({ page = 0, size = 20, status, type } = {}) => {
  const params = { page, size };
  if (status && status !== 'ALL') {
    params.status = status;
  }
  if (type && type !== 'ALL') {
    params.type = type;
  }
  const response = await api.get('/api/admin/accounts', { params });
  return response.data;
};

/**
 * List transactions with server-side filters and pagination.
 */
export const getAdminTransactions = async ({
  page = 0,
  size = 20,
  status,
  type,
  accountNumber,
} = {}) => {
  const params = { page, size };
  if (status && status !== 'ALL') {
    params.status = status;
  }
  if (type && type !== 'ALL') {
    params.type = type;
  }
  if (accountNumber && accountNumber.trim()) {
    params.accountNumber = accountNumber.trim();
  }
  const response = await api.get('/api/admin/transactions', { params });
  return response.data;
};

/**
 * Update an account's status (ACTIVE, FROZEN, CLOSED) - ADMIN only.
 */
export const updateAccountStatus = async (accountNumber, status) => {
  const response = await api.patch(`/api/admin/accounts/${accountNumber}/status`, { status });
  return response.data;
};

/**
 * List audit logs with pagination and optional action filter.
 */
export const getAdminAuditLogs = async ({ page = 0, size = 20, action } = {}) => {
  const params = { page, size };
  if (action && action !== 'ALL') {
    params.action = action;
  }
  const response = await api.get('/api/admin/audit-logs', { params });
  return response.data;
};
