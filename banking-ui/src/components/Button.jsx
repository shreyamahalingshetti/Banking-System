import React from 'react';
import Spinner from './Spinner';

export const Button = ({
  children,
  type = 'button',
  variant = 'primary',
  loading = false,
  disabled = false,
  onClick,
  className = '',
  ...props
}) => {
  const baseClasses =
    'inline-flex items-center justify-center font-medium rounded-lg px-4 py-2 text-sm transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-60 disabled:cursor-not-allowed';

  const variantClasses = {
    primary:
      'bg-indigo-600 hover:bg-indigo-700 text-white focus:ring-indigo-500 shadow-sm',
    secondary:
      'bg-slate-100 hover:bg-slate-200 text-slate-700 focus:ring-slate-400 border border-slate-300',
    danger:
      'bg-rose-600 hover:bg-rose-700 text-white focus:ring-rose-500 shadow-sm',
    outline:
      'bg-transparent hover:bg-indigo-50 text-indigo-600 border border-indigo-600 focus:ring-indigo-500',
  };

  return (
    <button
      type={type}
      disabled={disabled || loading}
      onClick={onClick}
      className={`${baseClasses} ${variantClasses[variant] || variantClasses.primary} ${className}`}
      {...props}
    >
      {loading && <Spinner size="sm" className="mr-2" />}
      {children}
    </button>
  );
};

export default Button;
