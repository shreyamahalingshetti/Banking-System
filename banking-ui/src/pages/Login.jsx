import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Input from '../components/Input';
import Button from '../components/Button';
import Alert from '../components/Alert';

export const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Check if there is a success message from registration redirect
  const successBanner = location.state?.successMessage;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email || !password) {
      setErrorMessage('Please enter both email and password.');
      return;
    }

    setLoading(true);
    try {
      const loggedInUser = await login(email, password);
      if (loggedInUser.role === 'ADMIN') {
        navigate('/admin', { replace: true });
      } else {
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      console.error('Login error:', err);
      if (err.response?.data?.message) {
        setErrorMessage(err.response.data.message);
      } else if (err.response?.status === 401) {
        setErrorMessage('Invalid email or password.');
      } else if (err.code === 'ERR_NETWORK') {
        setErrorMessage('Cannot connect to the banking server. Please check if the backend is running.');
      } else {
        setErrorMessage('Login failed. Please check your credentials and try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="mx-auto w-12 h-12 rounded-xl bg-indigo-600 flex items-center justify-center text-white text-2xl font-bold shadow-md">
          ₹
        </div>
        <h2 className="mt-4 text-3xl font-extrabold text-slate-900 tracking-tight">
          Enterprise Bank
        </h2>
        <p className="mt-2 text-sm text-slate-600">
          Sign in to access your secure accounts & transactions
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-lg rounded-2xl border border-slate-200">
          {successBanner && (
            <Alert type="success" message={successBanner} className="mb-4" />
          )}

          {errorMessage && (
            <Alert
              type="error"
              message={errorMessage}
              onClose={() => setErrorMessage('')}
              className="mb-4"
            />
          )}

          <form onSubmit={handleSubmit} noValidate>
            <Input
              label="Email address"
              id="email"
              name="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@domain.com"
              required
              autoComplete="email"
              disabled={loading}
            />

            <Input
              label="Password"
              id="password"
              name="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              autoComplete="current-password"
              disabled={loading}
            />

            <div className="mt-6">
              <Button
                type="submit"
                variant="primary"
                loading={loading}
                disabled={loading}
                className="w-full py-2.5 text-base"
              >
                Sign In
              </Button>
            </div>
          </form>

          <div className="mt-6 border-t border-slate-200 pt-6 text-center">
            <p className="text-sm text-slate-600">
              Don't have an account yet?{' '}
              <Link
                to="/register"
                className="font-medium text-indigo-600 hover:text-indigo-500 hover:underline"
              >
                Register as a customer
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
