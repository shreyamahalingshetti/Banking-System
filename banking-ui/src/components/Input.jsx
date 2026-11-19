import React from 'react';

export const Input = ({
  label,
  id,
  name,
  type = 'text',
  value,
  onChange,
  placeholder,
  error,
  required = false,
  disabled = false,
  className = '',
  autoComplete,
  ...props
}) => {
  const inputId = id || name;

  return (
    <div className="w-full mb-4">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-sm font-medium text-slate-700 mb-1"
        >
          {label} {required && <span className="text-rose-500">*</span>}
        </label>
      )}
      <input
        id={inputId}
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        autoComplete={autoComplete}
        className={`w-full px-3 py-2 text-sm bg-white border rounded-lg shadow-xs transition-colors duration-150 focus:outline-none focus:ring-2 disabled:bg-slate-50 disabled:text-slate-500 disabled:cursor-not-allowed ${
          error
            ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-200 text-rose-900'
            : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-200 text-slate-900'
        } ${className}`}
        {...props}
      />
      {error && (
        <p className="mt-1 text-xs text-rose-600 font-medium">{error}</p>
      )}
    </div>
  );
};

export default Input;
