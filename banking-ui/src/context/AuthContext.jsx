import React, { createContext, useContext, useState, useEffect } from 'react';
import { loginApi, registerApi } from '../api/auth';

const AuthContext = createContext(null);

export const parseJwt = (token) => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('token') || null);
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Sync/validate state on initial mount
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (savedToken && savedUser) {
      setToken(savedToken);
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        setUser(null);
      }
    } else {
      setToken(null);
      setUser(null);
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const data = await loginApi(email, password);
    const jwtClaims = parseJwt(data.token);

    // Build user representation
    const userData = {
      email: data.email || jwtClaims?.sub,
      role: data.role || jwtClaims?.role || 'CUSTOMER',
      customerId: jwtClaims?.customerId || null,
      fullName: user?.email === email && user?.fullName ? user.fullName : (data.email ? data.email.split('@')[0] : 'User'),
    };

    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(userData));
    setToken(data.token);
    setUser(userData);
    return userData;
  };

  const register = async (formData) => {
    const response = await registerApi(formData);
    return response;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const isAuthenticated = Boolean(token && user);
  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        loading,
        isAuthenticated,
        isAdmin,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
