import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/Button';

export const NotFound = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center items-center px-4 text-center">
      <div className="w-16 h-16 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold text-2xl mb-4">
        404
      </div>
      <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight mb-2">
        Page Not Found
      </h1>
      <p className="text-slate-600 max-w-md mb-6 text-sm">
        The page you are looking for doesn't exist or has been moved to another location.
      </p>
      <Link to="/dashboard">
        <Button variant="primary">
          Back to Dashboard
        </Button>
      </Link>
    </div>
  );
};

export default NotFound;
