import clsx from 'clsx';
import { forwardRef } from 'react';

const Input = forwardRef(({ 
  label,
  error,
  icon: Icon,
  className = '',
  type = 'text',
  ...props 
}, ref) => {
  const inputClasses = clsx(
    'input',
    {
      'input-error': error
    },
    className
  );

  return (
    <div className="input-group">
      {label && (
        <label className="input-label">
          {label}
        </label>
      )}
      <div className={clsx({ 'input-icon': Icon })}>
        {Icon && (
          <div className="input-icon-element">
            <Icon size={20} />
          </div>
        )}
        <input
          ref={ref}
          type={type}
          className={inputClasses}
          {...props}
        />
      </div>
      {error && (
        <div className="error-message">
          {error}
        </div>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
