import api from './axios';

/**
 * Fetch all accounts belonging to the authenticated customer.
 */
export const getMyAccounts = async () => {
  const response = await api.get('/api/accounts/me');
  return response.data;
};

/**
 * Create a new account with specified accountType for the authenticated customer.
 * Payload: { accountType: 'SAVINGS' | 'CURRENT' | 'SALARY' | 'FIXED_DEPOSIT', customerId }
 */
export const createAccount = async (accountType = 'SAVINGS', customerId = null) => {
  const body = {
    accountType,
    ...(customerId ? { customerId } : {}),
  };
  const response = await api.post('/api/accounts', body);
  return response.data;
};

/**
 * Fetch account details by 12-digit account number.
 */
export const getAccountByNumber = async (accountNumber) => {
  const response = await api.get(`/api/accounts/${accountNumber}`);
  return response.data;
};

/**
 * Deposit funds into an account.
 * Payload: { amount }
 */
export const deposit = async (accountNumber, amount) => {
  const response = await api.post(`/api/accounts/${accountNumber}/deposit`, { amount });
  return response.data;
};

/**
 * Withdraw funds from an account.
 * Payload: { amount }
 */
export const withdraw = async (accountNumber, amount) => {
  const response = await api.post(`/api/accounts/${accountNumber}/withdraw`, { amount });
  return response.data;
};

/**
 * Transfer funds between accounts.
 * Payload: { fromAccountNumber, toAccountNumber, amount, idempotencyKey }
 */
export const transferFunds = async ({ fromAccountNumber, toAccountNumber, amount, idempotencyKey }) => {
  const payload = {
    fromAccountNumber,
    toAccountNumber,
    amount,
    ...(idempotencyKey ? { idempotencyKey } : {}),
  };
  const response = await api.post('/api/transfers', payload);
  return response.data;
};

/**
 * Fetch transaction history for an account.
 */
export const getTransactions = async (accountNumber) => {
  const response = await api.get(`/api/accounts/${accountNumber}/transactions`);
  return response.data;
};

/**
 * Fetch transfer history for an account.
 */
export const getTransfers = async (accountNumber) => {
  const response = await api.get(`/api/accounts/${accountNumber}/transfers`);
  return response.data;
};
