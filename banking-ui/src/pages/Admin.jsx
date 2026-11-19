import React from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Button from '../components/Button';

export const Admin = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="pb-6 border-b border-slate-200 mb-8">
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">
            Admin Dashboard
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            System administration, user oversight, and global financial controls
          </p>
        </div>

        <div className="max-w-2xl bg-white rounded-2xl p-8 border border-purple-200 shadow-sm">
          <div className="flex items-center space-x-4 mb-6">
            <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-xl">
              🛡️
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-900">
                Admin Privileges Active
              </h2>
              <p className="text-sm text-slate-500">
                You are authenticated with full administrative access to enterprise resources.
              </p>
            </div>
          </div>

          <p className="text-sm text-slate-600 mb-6 leading-relaxed">
            Administrative endpoints (analytics, audit logs, and account freeze/unfreeze controls)
            are fully secured on the backend. The complete management dashboard UI will be connected here.
          </p>

          <div className="flex items-center space-x-4">
            <Link to="/dashboard">
              <Button variant="primary">
                Go to Customer Dashboard
              </Button>
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Admin;
