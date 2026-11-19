import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Button from '../components/Button';
import Input from '../components/Input';
import Alert from '../components/Alert';
import StatusBadge from '../components/StatusBadge';
import Spinner from '../components/Spinner';
import Modal from '../components/Modal';
import TransactionTable from '../components/TransactionTable';
import {
  getAccountByNumber,
  deposit,
  withdraw,
  transferFunds,
  getTransactions,
} from '../api/accounts';
import { formatCurrency, formatAccountNumber, formatDateTime } from '../utils/formatters';

export const AccountDetails = () => {
  const { accountNumber } = useParams();

  // Account and transactions state
  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loadingAccount, setLoadingAccount] = useState(true);
  const [loadingTransactions, setLoadingTransactions] = useState(true);
  const [accessError, setAccessError] = useState(null);

  // Active Tab: 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER'
  const [activeTab, setActiveTab] = useState('DEPOSIT');

  // Operation notifications
  const [operationSuccess, setOperationSuccess] = useState('');
  const [operationError, setOperationError] = useState('');

  // Form states
  const [depositAmount, setDepositAmount] = useState('');
  const [depositError, setDepositError] = useState('');
  const [depositPending, setDepositPending] = useState(false);

  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [withdrawError, setWithdrawError] = useState('');
  const [withdrawPending, setWithdrawPending] = useState(false);

  const [transferRecipient, setTransferRecipient] = useState('');
  const [transferAmount, setTransferAmount] = useState('');
  const [transferErrors, setTransferErrors] = useState({});
  const [transferPending, setTransferPending] = useState(false);
  const [showTransferConfirm, setShowTransferConfirm] = useState(false);
  const [idempotencyKey, setIdempotencyKey] = useState(() =>
    typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : String(Date.now())
  );

  // Client validation helper for amounts
  const validateAmount = (value) => {
    if (!value || String(value).trim() === '') {
      return 'Amount is required';
    }
    const str = String(value).trim();
    // Validate positive decimal number format with up to 2 decimal places
    const validFormat = /^\d+(\.\d{1,2})?$/;
    if (!validFormat.test(str)) {
      return 'Amount must be a valid positive number with at most 2 decimal places';
    }
    const num = Number(str);
    if (isNaN(num) || num <= 0) {
      return 'Amount must be greater than 0';
    }
    return null;
  };

  // Fetch account details
  const fetchAccount = useCallback(async () => {
    try {
      setLoadingAccount(true);
      setAccessError(null);
      const data = await getAccountByNumber(accountNumber);
      setAccount(data);
    } catch (err) {
      console.error('Failed to load account:', err);
      if (err.response?.status === 403) {
        setAccessError(
          err.response?.data?.message ||
            'Access denied: You do not have permission to view or manage this account.'
        );
      } else if (err.response?.status === 404) {
        setAccessError('Account not found with account number: ' + accountNumber);
      } else {
        setAccessError(
          err.response?.data?.message ||
            'Failed to load account details. Please check your connection and try again.'
        );
      }
    } finally {
      setLoadingAccount(false);
    }
  }, [accountNumber]);

  // Fetch transaction history
  const fetchTransactions = useCallback(async () => {
    try {
      setLoadingTransactions(true);
      const data = await getTransactions(accountNumber);
      setTransactions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load transactions:', err);
    } finally {
      setLoadingTransactions(false);
    }
  }, [accountNumber]);

  // Initial load
  useEffect(() => {
    fetchAccount();
    fetchTransactions();
  }, [fetchAccount, fetchTransactions]);

  // Handle Tab switch
  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setOperationSuccess('');
    setOperationError('');
    setDepositError('');
    setWithdrawError('');
    setTransferErrors({});
    if (tab === 'TRANSFER' && !idempotencyKey) {
      setIdempotencyKey(
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : String(Date.now())
      );
    }
  };

  const isAccountActive = account?.status === 'ACTIVE';

  // ---------------- Deposit Handler ----------------
  const handleDepositSubmit = async (e) => {
    e.preventDefault();
    if (depositPending || !isAccountActive) return;

    setOperationSuccess('');
    setOperationError('');
    setDepositError('');

    const error = validateAmount(depositAmount);
    if (error) {
      setDepositError(error);
      return;
    }

    setDepositPending(true);
    try {
      const result = await deposit(accountNumber, depositAmount.trim());
      setOperationSuccess(
        `Successfully deposited ${formatCurrency(result.amount)}! (Tx ID: ${result.transactionId})`
      );
      setDepositAmount('');
      // Refresh balance and transactions without page reload
      await Promise.all([fetchAccount(), fetchTransactions()]);
    } catch (err) {
      console.error('Deposit error:', err);
      setOperationError(
        err.response?.data?.message ||
          'Deposit failed. Please verify the amount and try again.'
      );
      // Refresh transactions to show any failed transaction records
      fetchTransactions();
    } finally {
      setDepositPending(false);
    }
  };

  // ---------------- Withdraw Handler ----------------
  const handleWithdrawSubmit = async (e) => {
    e.preventDefault();
    if (withdrawPending || !isAccountActive) return;

    setOperationSuccess('');
    setOperationError('');
    setWithdrawError('');

    const error = validateAmount(withdrawAmount);
    if (error) {
      setWithdrawError(error);
      return;
    }

    setWithdrawPending(true);
    try {
      const result = await withdraw(accountNumber, withdrawAmount.trim());
      setOperationSuccess(
        `Successfully withdrew ${formatCurrency(result.amount)}! (Tx ID: ${result.transactionId})`
      );
      setWithdrawAmount('');
      // Refresh balance and transactions without page reload
      await Promise.all([fetchAccount(), fetchTransactions()]);
    } catch (err) {
      console.error('Withdrawal error:', err);
      setOperationError(
        err.response?.data?.message ||
          'Withdrawal failed. Please check your available balance.'
      );
      // Refresh transactions to capture failed audit records
      fetchTransactions();
    } finally {
      setWithdrawPending(false);
    }
  };

  // ---------------- Transfer Form Validation ----------------
  const validateTransferForm = () => {
    const errors = {};
    const trimmedRecipient = String(transferRecipient || '').trim();

    if (!trimmedRecipient) {
      errors.recipient = 'Recipient account number is required';
    } else if (!/^\d{12}$/.test(trimmedRecipient)) {
      errors.recipient = 'Recipient account number must be exactly 12 digits';
    } else if (trimmedRecipient === accountNumber) {
      errors.recipient = 'Cannot transfer funds to the same account';
    }

    const amountErr = validateAmount(transferAmount);
    if (amountErr) {
      errors.amount = amountErr;
    }

    return errors;
  };

  // Initiate Transfer: open confirmation step
  const handleTransferInitiate = (e) => {
    e.preventDefault();
    if (transferPending || !isAccountActive) return;

    setOperationSuccess('');
    setOperationError('');
    setTransferErrors({});

    const errors = validateTransferForm();
    if (Object.keys(errors).length > 0) {
      setTransferErrors(errors);
      return;
    }

    // Show confirmation modal
    setShowTransferConfirm(true);
  };

  // Confirm Transfer execution
  const handleTransferConfirm = async () => {
    if (transferPending) return;

    setTransferPending(true);
    try {
      const result = await transferFunds({
        fromAccountNumber: accountNumber,
        toAccountNumber: transferRecipient.trim(),
        amount: transferAmount.trim(),
        idempotencyKey,
      });

      setShowTransferConfirm(false);
      setOperationSuccess(
        `Transfer of ${formatCurrency(result.amount)} completed successfully! (Transaction ID: ${result.transactionId})`
      );
      setTransferAmount('');
      setTransferRecipient('');
      // Generate a new idempotencyKey for future transfers
      setIdempotencyKey(
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : String(Date.now())
      );
      // Refresh balance and transactions
      await Promise.all([fetchAccount(), fetchTransactions()]);
    } catch (err) {
      console.error('Transfer error:', err);
      setShowTransferConfirm(false);
      setOperationError(
        err.response?.data?.message ||
          'Transfer failed. Please check the recipient account number and balance.'
      );
      // Do NOT regenerate idempotencyKey so retries safely reuse the same key!
      fetchTransactions();
    } finally {
      setTransferPending(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Breadcrumb / Back Link */}
        <div className="mb-6 flex items-center justify-between">
          <Link
            to="/dashboard"
            className="inline-flex items-center text-sm font-medium text-indigo-600 hover:text-indigo-800 transition-colors"
          >
            <svg
              className="w-4 h-4 mr-1.5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Back to Accounts
          </Link>
          <div className="text-xs text-slate-400">
            Account Management & Ledger
          </div>
        </div>

        {/* Access Denied or Loading Error */}
        {accessError ? (
          <div className="max-w-xl mx-auto my-12 bg-white rounded-2xl p-8 border border-rose-200 shadow-sm text-center">
            <div className="w-14 h-14 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Access Denied</h2>
            <p className="text-sm text-slate-600 mb-6">{accessError}</p>
            <Link to="/dashboard">
              <Button variant="primary">Return to My Dashboard</Button>
            </Link>
          </div>
        ) : loadingAccount ? (
          <div className="p-16 text-center">
            <Spinner size="lg" className="text-indigo-600 mb-3" />
            <p className="text-sm text-slate-500">Loading account details...</p>
          </div>
        ) : !account ? (
          <div className="text-center py-12">
            <Alert type="error" message="Account could not be loaded." />
          </div>
        ) : (
          <>
            {/* Account Overview Header Card */}
            <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 shadow-sm mb-8">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
                <div>
                  <div className="flex items-center space-x-3 mb-2">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                      Savings Account
                    </span>
                    <StatusBadge status={account.status} />
                  </div>
                  <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight font-mono">
                    {formatAccountNumber(account.accountNumber)}
                  </h1>
                  <p className="text-xs text-slate-500 mt-1">
                    Opened on {formatDateTime(account.createdAt)}
                  </p>
                </div>

                <div className="bg-slate-50 rounded-xl p-4 sm:p-5 border border-slate-200 md:text-right">
                  <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 block mb-1">
                    Available Balance
                  </span>
                  <div className="text-3xl sm:text-4xl font-black text-indigo-900 tracking-tight">
                    {formatCurrency(account.balance)}
                  </div>
                </div>
              </div>
            </div>

            {/* Non-Active Account Warning Banner */}
            {!isAccountActive && (
              <div className="mb-8">
                <Alert
                  type="warning"
                  message={`Account status is currently ${account.status}. Deposits, withdrawals, and fund transfers are disabled for this account.`}
                />
              </div>
            )}

            {/* Operations Section */}
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden mb-8">
              {/* Tabs Bar */}
              <div className="border-b border-slate-200 bg-slate-50/50 px-4 sm:px-6">
                <nav className="flex space-x-8" aria-label="Account Operations">
                  <button
                    onClick={() => handleTabChange('DEPOSIT')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm sm:text-base transition-colors ${
                      activeTab === 'DEPOSIT'
                        ? 'border-indigo-600 text-indigo-600 font-semibold'
                        : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                    }`}
                  >
                    Deposit Funds
                  </button>
                  <button
                    onClick={() => handleTabChange('WITHDRAW')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm sm:text-base transition-colors ${
                      activeTab === 'WITHDRAW'
                        ? 'border-indigo-600 text-indigo-600 font-semibold'
                        : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                    }`}
                  >
                    Withdraw Funds
                  </button>
                  <button
                    onClick={() => handleTabChange('TRANSFER')}
                    className={`py-4 px-1 border-b-2 font-medium text-sm sm:text-base transition-colors ${
                      activeTab === 'TRANSFER'
                        ? 'border-indigo-600 text-indigo-600 font-semibold'
                        : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                    }`}
                  >
                    Transfer Funds
                  </button>
                </nav>
              </div>

              {/* Tab Content Panel */}
              <div className="p-6 sm:p-8">
                {operationSuccess && (
                  <Alert
                    type="success"
                    message={operationSuccess}
                    onClose={() => setOperationSuccess('')}
                    className="mb-6"
                  />
                )}

                {operationError && (
                  <Alert
                    type="error"
                    message={operationError}
                    onClose={() => setOperationError('')}
                    className="mb-6"
                  />
                )}

                {/* ---------------- DEPOSIT TAB ---------------- */}
                {activeTab === 'DEPOSIT' && (
                  <div className="max-w-md">
                    <h3 className="text-lg font-bold text-slate-900 mb-1">
                      Deposit Money
                    </h3>
                    <p className="text-xs text-slate-500 mb-6">
                      Add funds directly to account {formatAccountNumber(account.accountNumber)}
                    </p>

                    <form onSubmit={handleDepositSubmit} noValidate>
                      <Input
                        label="Deposit Amount (₹)"
                        id="depositAmount"
                        name="depositAmount"
                        type="text"
                        value={depositAmount}
                        onChange={(e) => {
                          setDepositAmount(e.target.value);
                          if (depositError) setDepositError('');
                        }}
                        placeholder="e.g. 5000.00"
                        required
                        disabled={!isAccountActive || depositPending}
                        error={depositError}
                      />

                      <div className="mt-6">
                        <Button
                          type="submit"
                          variant="primary"
                          loading={depositPending}
                          disabled={!isAccountActive || depositPending}
                          className="w-full sm:w-auto min-w-[140px]"
                        >
                          Confirm Deposit
                        </Button>
                      </div>
                    </form>
                  </div>
                )}

                {/* ---------------- WITHDRAW TAB ---------------- */}
                {activeTab === 'WITHDRAW' && (
                  <div className="max-w-md">
                    <h3 className="text-lg font-bold text-slate-900 mb-1">
                      Withdraw Money
                    </h3>
                    <p className="text-xs text-slate-500 mb-6">
                      Withdraw available funds from your account
                    </p>

                    <form onSubmit={handleWithdrawSubmit} noValidate>
                      <Input
                        label="Withdrawal Amount (₹)"
                        id="withdrawAmount"
                        name="withdrawAmount"
                        type="text"
                        value={withdrawAmount}
                        onChange={(e) => {
                          setWithdrawAmount(e.target.value);
                          if (withdrawError) setWithdrawError('');
                        }}
                        placeholder="e.g. 1500.00"
                        required
                        disabled={!isAccountActive || withdrawPending}
                        error={withdrawError}
                      />

                      <div className="mt-6">
                        <Button
                          type="submit"
                          variant="primary"
                          loading={withdrawPending}
                          disabled={!isAccountActive || withdrawPending}
                          className="w-full sm:w-auto min-w-[140px]"
                        >
                          Confirm Withdrawal
                        </Button>
                      </div>
                    </form>
                  </div>
                )}

                {/* ---------------- TRANSFER TAB ---------------- */}
                {activeTab === 'TRANSFER' && (
                  <div className="max-w-lg">
                    <h3 className="text-lg font-bold text-slate-900 mb-1">
                      Atomic Fund Transfer
                    </h3>
                    <p className="text-xs text-slate-500 mb-6">
                      Transfer money instantly to another bank account with double-entry ledger protection
                    </p>

                    <form onSubmit={handleTransferInitiate} noValidate>
                      <div className="mb-4">
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                          Source Account
                        </label>
                        <input
                          type="text"
                          disabled
                          value={`${formatAccountNumber(account.accountNumber)} (Available: ${formatCurrency(account.balance)})`}
                          className="w-full px-3 py-2 text-sm bg-slate-100 border border-slate-300 rounded-lg text-slate-600 font-mono"
                        />
                      </div>

                      <Input
                        label="Recipient Account Number"
                        id="transferRecipient"
                        name="transferRecipient"
                        type="text"
                        value={transferRecipient}
                        onChange={(e) => {
                          setTransferRecipient(e.target.value);
                          if (transferErrors.recipient) {
                            setTransferErrors((prev) => ({ ...prev, recipient: '' }));
                          }
                        }}
                        placeholder="12-digit account number"
                        required
                        disabled={!isAccountActive || transferPending}
                        error={transferErrors.recipient}
                      />

                      <Input
                        label="Transfer Amount (₹)"
                        id="transferAmount"
                        name="transferAmount"
                        type="text"
                        value={transferAmount}
                        onChange={(e) => {
                          setTransferAmount(e.target.value);
                          if (transferErrors.amount) {
                            setTransferErrors((prev) => ({ ...prev, amount: '' }));
                          }
                        }}
                        placeholder="e.g. 2500.00"
                        required
                        disabled={!isAccountActive || transferPending}
                        error={transferErrors.amount}
                      />

                      <div className="mt-6">
                        <Button
                          type="submit"
                          variant="primary"
                          loading={transferPending}
                          disabled={!isAccountActive || transferPending}
                          className="w-full sm:w-auto min-w-[140px]"
                        >
                          Review & Transfer
                        </Button>
                      </div>
                    </form>
                  </div>
                )}
              </div>
            </div>

            {/* Transaction History Table */}
            <TransactionTable
              transactions={transactions}
              loading={loadingTransactions}
              onRetry={fetchTransactions}
            />
          </>
        )}
      </main>

      {/* Transfer Confirmation Modal */}
      <Modal
        isOpen={showTransferConfirm}
        onClose={() => setShowTransferConfirm(false)}
        title="Confirm Fund Transfer"
        confirmText="Confirm & Send"
        cancelText="Cancel"
        onConfirm={handleTransferConfirm}
        loading={transferPending}
      >
        <div className="space-y-4">
          <p className="text-slate-600 text-sm">
            Please verify the transaction details below before proceeding:
          </p>
          <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-500">From Account:</span>
              <span className="font-mono font-semibold text-slate-800">
                {formatAccountNumber(account?.accountNumber)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">To Recipient:</span>
              <span className="font-mono font-semibold text-slate-800">
                {formatAccountNumber(transferRecipient)}
              </span>
            </div>
            <div className="flex justify-between border-t border-slate-200 pt-2 text-base font-bold">
              <span className="text-slate-700">Amount:</span>
              <span className="text-indigo-600">
                {formatCurrency(transferAmount)}
              </span>
            </div>
          </div>
          <p className="text-xs text-slate-400">
            This transfer is atomic and idempotent. The funds will be transferred immediately upon confirmation.
          </p>
        </div>
      </Modal>
    </div>
  );
};

export default AccountDetails;
