import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Input from '../components/Input';
import Button from '../components/Button';
import Alert from '../components/Alert';

export const Register = () => {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const [fieldErrors, setFieldErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [loading, setLoading] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    // Clear field-level error on change
    if (fieldErrors[name]) {
      setFieldErrors((prev) => {
        const next = { ...prev };
        delete next[name];
        return next;
      });
    }
    if (generalError) {
      setGeneralError('');
    }
  };

  const validateClientSide = () => {
    const errors = {};
    if (!formData.fullName.trim()) {
      errors.fullName = 'Full name is required';
    }

    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      errors.password = 'Password must be at least 8 characters long';
    }

    if (!formData.confirmPassword) {
      errors.confirmPassword = 'Confirm password is required';
    } else if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    return errors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    setFieldErrors({});

    const errors = validateClientSide();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setLoading(true);
    try {
      await register({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        password: formData.password,
      });

      // Save user fullName in localStorage helper map so it persists for this customer
      try {
        const cachedUser = { email: formData.email.trim(), fullName: formData.fullName.trim() };
        localStorage.setItem('user', JSON.stringify(cachedUser));
      } catch {
        // ignore
      }

      // Navigate to login with success banner
      navigate('/login', {
        state: {
          successMessage: 'Registration successful! Please sign in with your new account credentials.',
        },
      });
    } catch (err) {
      console.error('Registration error:', err);
      if (err.response?.status === 409) {
        // Email already registered
        setGeneralError(err.response?.data?.message || 'Email already registered. Please sign in or use another email.');
      } else if (err.response?.status === 400 && err.response?.data?.validationErrors) {
        // Field-level validation errors from Spring Boot
        setFieldErrors(err.response.data.validationErrors);
      } else if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else if (err.code === 'ERR_NETWORK') {
        setGeneralError('Cannot connect to backend server. Please verify the service is running.');
      } else {
        setGeneralError('Registration failed. Please check your information and try again.');
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
          Create an Account
        </h2>
        <p className="mt-2 text-sm text-slate-600">
          Join Enterprise Bank and manage your funds with confidence
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-lg rounded-2xl border border-slate-200">
          {generalError && (
            <Alert
              type="error"
              message={generalError}
              onClose={() => setGeneralError('')}
              className="mb-4"
            />
          )}

          <form onSubmit={handleSubmit} noValidate>
            <Input
              label="Full Name"
              id="fullName"
              name="fullName"
              type="text"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="Aditi Sharma"
              required
              autoComplete="name"
              error={fieldErrors.fullName}
              disabled={loading}
            />

            <Input
              label="Email address"
              id="email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="aditi@example.com"
              required
              autoComplete="email"
              error={fieldErrors.email}
              disabled={loading}
            />

            <Input
              label="Password"
              id="password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="At least 8 characters"
              required
              autoComplete="new-password"
              error={fieldErrors.password}
              disabled={loading}
            />

            <Input
              label="Confirm Password"
              id="confirmPassword"
              name="confirmPassword"
              type="password"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Re-enter password"
              required
              autoComplete="new-password"
              error={fieldErrors.confirmPassword}
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
                Complete Registration
              </Button>
            </div>
          </form>

          <div className="mt-6 border-t border-slate-200 pt-6 text-center">
            <p className="text-sm text-slate-600">
              Already have an account?{' '}
              <Link
                to="/login"
                className="font-medium text-indigo-600 hover:text-indigo-500 hover:underline"
              >
                Sign in here
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
