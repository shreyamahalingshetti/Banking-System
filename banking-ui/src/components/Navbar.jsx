import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Button from './Button';

export const Navbar = () => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-white border-b border-slate-200 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          {/* Logo / App Name */}
          <div className="flex items-center space-x-3">
            <Link to="/dashboard" className="flex items-center space-x-2">
              <div className="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold text-lg shadow-sm">
                ₹
              </div>
              <span className="text-xl font-bold text-slate-900 tracking-tight">
                Enterprise Bank
              </span>
            </Link>
            {isAdmin && (
              <span className="hidden sm:inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-purple-100 text-purple-800 border border-purple-200">
                Admin Mode
              </span>
            )}
          </div>

          {/* User info & Logout */}
          <div className="flex items-center space-x-4">
            {user && (
              <div className="flex items-center space-x-3">
                <div className="text-right hidden sm:block">
                  <div className="text-sm font-semibold text-slate-800">
                    {user.fullName || user.email}
                  </div>
                  <div className="text-xs text-slate-500 font-mono">
                    {user.email}
                  </div>
                </div>

                <span
                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                    isAdmin
                      ? 'bg-purple-100 text-purple-800 border border-purple-200'
                      : 'bg-indigo-100 text-indigo-800 border border-indigo-200'
                  }`}
                >
                  {user.role || 'CUSTOMER'}
                </span>

                {isAdmin && (
                  <Link
                    to="/admin"
                    className="text-xs text-indigo-600 hover:text-indigo-800 font-medium hidden md:inline-block"
                  >
                    Admin Panel
                  </Link>
                )}

                <Button
                  variant="secondary"
                  onClick={handleLogout}
                  className="text-xs px-3 py-1.5"
                >
                  Logout
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
